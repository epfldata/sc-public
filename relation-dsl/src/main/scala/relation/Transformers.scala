package relation

import squid.ir._
import squid.lib.{Var, transparencyPropagating, transparent}
import squid.quasi.{embed, phase}

object RelationInliner extends RelationDSL.Lowering('RelRemove)

object RelationCtorInliner extends RelationDSL.SelfTransformer with SimpleRuleBasedTransformer {
  import RelationDSL.Predef._
  /*_*/
  rewrite {
    case ir"val $r = new Relation($sch, $under); $body: $t" =>
      val newBody = body rewrite {
        case ir"$$r.schema" => sch
        case ir"$$r.underlying" => under
      }
      newBody subs 'r -> Abort()

  }
}

object RelationLowering extends RelationDSL.TransformerWrapper(RelationInliner, RelationCtorInliner) with BottomUpTransformer with FixPointTransformer

object SchemaSpecialization extends RelationDSL.SelfTransformer with FixPointRuleBasedTransformer with TopDownTransformer with FixPointTransformer {
  import RelationDSL.Predef._
  rewrite {
    case ir"new Schema(List[String]($columns*))" =>
      ir"Schema($columns*)"
  }
  rewrite {
    case ir"val $s = Schema($columns*); $body: $t" =>
      //println(s"transforming $columns, $body")
      val newBody = body fix_rewrite {
        case ir"($r: Row).getField($$s, $name)" if columns.contains(name) =>
          val index = columns.zipWithIndex.find(_._1 == name).get._2
          //println(s"found $name in $columns, index: $index");
          ir"$r.getValue(${Const(index)})"
        case ir"$$s.columns" => ir"List($columns*)"
        case ir"$$s.indicesOf(List[String]($columns2*))" =>
          val cols = columns:Seq[IR[String,Nothing]] // TODO better interface
          val columnIndexMap = cols.zipWithIndex.toMap
          val indices = columns2.map(x => columnIndexMap(x)).map(x => Const(x))
          ir"List($indices*)"
      }
      newBody subs 's -> Abort()
      //ir"val s = Schema($columns*); $newBody"
  }

  rewrite {
    case ir"List[$t]($elems*).zip($l2: List[$t2])" =>
      val newElems = elems.zipWithIndex.map(x => ir"(${x._1}, $l2(${Const(x._2)}))")
      ir"List($newElems*)"
  }
  rewrite {
    case ir"List[$t]($elems*).zipWithIndex" =>
      val newElems = elems.zipWithIndex.map(x => ir"(${x._1}, ${Const(x._2)})")
      ir"List($newElems*)"
  }
  rewrite {
    case ir"List[$t]($elems*).map($f: t => $t2)" =>
      val newElems = elems.map(x => ir"$f($x)")
      ir"List($newElems*)"
  }
  rewrite {
    case ir"List[$t]($elems*).size" =>
      Const(elems.size)
  }
  rewrite {
    case ir"List[$t]($elems1*) ++ List[t]($elems2*)" =>
      val newElems = elems1 ++ elems2
      ir"List($newElems*)"
  }
}

object ListFusion extends RelationDSL.TransformerWrapper(ListFusionTransformers.ListToStream, ListFusionTransformers.StreamFusion, ListFusionTransformers.StreamLowering) with BottomUpTransformer

@embed
class Stream[T](val consume: (T => Unit) => Unit) {
  @phase('StreamInline)
  def map[S](f: T => S): Stream[S] = new Stream(k => foreach(e => k(f(e))))
  @phase('StreamInline)
  def filter(p: T => Boolean): Stream[T] = new Stream(k => foreach(e => if(p(e)) k(e)))
  @phase('StreamInline)
  def foreach(f: T => Unit): Unit = consume(f)
  def toList: List[T] = ???
}
object Stream {
  @phase('StreamInline)
  def fromList[T](l: List[T]): Stream[T] = new Stream(k => l.foreach(k))
}

object ListFusionTransformers {
  object ListToStream extends RelationDSL.SelfTransformer with FixPointRuleBasedTransformer with TopDownTransformer with FixPointTransformer {
    import RelationDSL.Predef._

    rewrite {
      case ir"($l: List[$t]).map($f: t => $t2)" =>
        ir"Stream.fromList($l).map($f).toList"
    }

    rewrite {
      case ir"($l: List[$t]).filter($p)" =>
        ir"Stream.fromList($l).filter($p).toList"
    }

    rewrite {
      case ir"($l: List[$t]).foreach($f: t => Unit)" =>
        ir"Stream.fromList($l).foreach($f)"
    }
  }

  object StreamFusion extends RelationDSL.SelfTransformer with FixPointRuleBasedTransformer with TopDownTransformer with FixPointTransformer {
    import RelationDSL.Predef._

    rewrite {
      case ir"Stream.fromList(($s: Stream[$t]).toList)" =>
        ir"$s"
    }
  }

  object StreamInliner extends RelationDSL.Lowering('StreamInline)


  object StreamCtorInliner extends RelationDSL.SelfTransformer with SimpleRuleBasedTransformer {
    import RelationDSL.Predef._
    rewrite {
      case ir"val $s = new Stream($consume: ($t => Unit) => Unit); $body: $t2" =>
        val newBody = body rewrite {
          case ir"$$s.consume" => consume
        }
        newBody subs 's -> {throw RewriteAbort()}

    }
  }

  object StreamLowering extends RelationDSL.TransformerWrapper(StreamInliner, StreamCtorInliner) with BottomUpTransformer with FixPointTransformer
}

class TupledRow(val tup: Product) {
  @transparencyPropagating
  def toRow: Row = ???
  @transparencyPropagating
  def getElem(idx: Int): String = ???
}

object TupledRow {
  @transparencyPropagating
  def fromRow(r: Row): TupledRow = ???
  @transparencyPropagating
  def apply(p: Product): TupledRow = ???
}

object RowLayout extends RelationDSL.TransformerWrapper(RowLayoutTransformers.RowToTupledRow, RowLayoutTransformers.TupledRowFusion, RowLayoutTransformers.TupledRowLowering) with BottomUpTransformer

object RowLayoutTransformers {
  import RelationDSL.Predef._

  object TupleProcessing {
    def getTupleType(arity: Int): IRType[_] = {
      arity match {
        case 2 => irTypeOf[(String, String)]
        case 3 => irTypeOf[(String, String, String)]
        case 4 => irTypeOf[(String, String, String, String)]
        case _ => throw new Exception(s"Does not support getting the type a tuple of $arity elements.")
      }
    }

    def constructTuple2[C](f: Int => IR[String, C], arity: Int): IR[_, C] = {
      arity match {
        case 2 => ir"(${f(0)}, ${f(1)})"
        case 3 => ir"(${f(0)}, ${f(1)}, ${f(2)})"
        case 4 => ir"(${f(0)}, ${f(1)}, ${f(2)}, ${f(3)})"
        case _ => throw new Exception(s"Does not support the construction of a tuple of $arity elements.")
      }
    }

    def constructTuple[C <: AnyRef](elems: IR[List[String], C], arity: Int): IR[_, C] = {
      constructTuple2[C]((idx: Int) => ir"$elems(${Const(idx)})", arity)
    }

    def getTupleArity(tup:IR[Any,_]): Int = {
      tup match {
        case ir"$tup: ($ta,$tb)" => 2
        case ir"$tup: ($ta,$tb,$tc)" => 3
        case ir"$tup: ($ta,$tb,$tc,$td)" => 4
        case _ => throw new Exception(s"Does not support getting the arity of the tuple `$tup`.")
      }
    }

    def projectTuple[C](tup:IR[Product,C], idx:Int) = {
      val res = tup match {
        case ir"$tup: ($ta,$tb)" => idx match {
          case 0 => ir"$tup._1"
          case 1 => ir"$tup._2"
        }
        case ir"$tup: ($ta,$tb,$tc)" => idx match {
          case 0 => ir"$tup._1"
          case 1 => ir"$tup._2"
          case 2 => ir"$tup._3"
        }
        case ir"$tup: ($ta,$tb,$tc, $td)" => idx match {
          case 0 => ir"$tup._1"
          case 1 => ir"$tup._2"
          case 2 => ir"$tup._3"
          case 3 => ir"$tup._4"
        }
        case ir"$tup: Product" => throw new Exception(s"Does not support the projection of the ${idx}th element of the tuple `$tup`.")
      }
      ir"$res.asInstanceOf[String]"
    }
  }

  import TupleProcessing._

  object RowToTupledRow extends RelationDSL.SelfTransformer with SimpleRuleBasedTransformer with BottomUpTransformer with FixPointTransformer {


    def listRewrite[T:IRType,C](list: IR[Var[List[Row]],C{val list: Var[List[Row]]}], body: IR[T,C{val list: Var[List[Row]]}]): IR[T,C] = {
      var size: Int = -1
      body analyse {
        case ir"$$list := ($$list.!).::(Row($_, ${Const(s)}))" =>
          size = s
      }
      getTupleType(size) match { case tupType: IRType[tp] =>
        val newList = ir"newList? : Var[List[$tupType]]"
        val body0 = body rewrite {
          case ir"$$list := ($$list.!).::(Row($elems, ${Const(s)}))" =>
            ir"$newList := ($newList.!).::(${constructTuple(elems, s)}.asInstanceOf[$tupType])"
          case ir"$$list.!.foreach[$t](x => $fbody)" =>  ir"($newList.!) foreach {e => val x = TupledRow(e.asInstanceOf[Product]).toRow; $fbody}"
          //      case ir"$$list.!.foreach[$t]($f)" =>  ir"($newList.!) foreach $f"
        }
        val body1 = body0 subs 'list -> {throw RewriteAbort()}
        ir"val newList: Var[List[$tupType]] = Var(Nil); $body1"
      }}

    rewrite {
      case ir"($r: Row).getValue($idx)" =>
        ir"TupledRow.fromRow($r).getElem($idx)"
      case ir"val $list: Var[List[Row]] = Var(Nil); $body: $t2" =>
        listRewrite(list, body)
    }
  }

  object TupledRowFusion extends RelationDSL.SelfTransformer with SimpleRuleBasedTransformer with BottomUpTransformer with FixPointTransformer {
    rewrite {
      case ir"TupledRow.fromRow(($t: TupledRow).toRow)" =>
        t
    }
  }

  object TupledRowLowering extends RelationDSL.SelfTransformer with SimpleRuleBasedTransformer with BottomUpTransformer with FixPointTransformer {
    rewrite {
      case ir"TupledRow($e).getElem(${Const(idx)})" =>
        projectTuple(e, idx)
      case ir"TupledRow($e).toRow" =>
        val arity = getTupleArity(e)
        val elems = (0 until arity).map(i => projectTuple(e, i))
        ir"Row(List($elems*), ${Const(arity)})"
    }
  }
}

