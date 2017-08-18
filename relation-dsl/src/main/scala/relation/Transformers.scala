package relation

import squid.ir._
import squid.lang.{InspectableBase, ScalaCore}
import squid.lib.{Var, transparencyPropagating, transparent}
import squid.quasi.{embed, phase}
import squid.anf.transfo.TupleNormalizer

import scala.collection.mutable.{HashMap, ArrayBuffer}

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

object RowLayout extends RelationDSL.TransformerWrapper(
    RowLayoutTransformers.RowToTupledRow
    , RowLayoutTransformers.TupledRowFusion
    , RowLayoutTransformers.TupledRowLowering
  ) with TopDownTransformer

// TODO move it to the core optimizations.
trait TupleNNormalizer extends TupleNormalizer { self =>
  val base: InspectableBase with ScalaCore
  import base.Predef._

  rewrite {
    case ir"($a:$ta,$b:$tb,$c:$tc)._1" => ir"$a"
    case ir"($a:$ta,$b:$tb,$c:$tc)._2" => ir"$b"
    case ir"($a:$ta,$b:$tb,$c:$tc)._3" => ir"$c"
    case ir"($a:$ta,$b:$tb,$c:$tc,$d:$td)._1" => ir"$a"
    case ir"($a:$ta,$b:$tb,$c:$tc,$d:$td)._2" => ir"$b"
    case ir"($a:$ta,$b:$tb,$c:$tc,$d:$td)._3" => ir"$c"
    case ir"($a:$ta,$b:$tb,$c:$tc,$d:$td)._4" => ir"$d"
  }

}

object TupleProcessing {
  import RelationDSL.Predef._
  def getTupleType(arity: Int): IRType[_] = {
    arity match {
      case 2 => irTypeOf[(String, String)]
      case 3 => irTypeOf[(String, String, String)]
      case 4 => irTypeOf[(String, String, String, String)]
      case _ => throw new Exception(s"Does not support getting the type a tuple of $arity elements.")
    }
  }

  def constructTuple[C](f: Int => IR[String, C], arity: Int): IR[_, C] = {
    arity match {
      case 2 => ir"(${f(0)}, ${f(1)})"
      case 3 => ir"(${f(0)}, ${f(1)}, ${f(2)})"
      case 4 => ir"(${f(0)}, ${f(1)}, ${f(2)}, ${f(3)})"
      case _ => throw new Exception(s"Does not support the construction of a tuple of $arity elements.")
    }
  }

  def constructTupleFromList[C <: AnyRef](elems: IR[List[String], C], arity: Int): IR[_, C] = {
    constructTuple[C]((idx: Int) => ir"$elems(${Const(idx)})", arity)
  }

  def getTupleArity(tup:IR[Any,_]): Int = {
    tup match {
      case ir"$tup: ($ta,$tb)" => 2
      case ir"$tup: ($ta,$tb,$tc)" => 3
      case ir"$tup: ($ta,$tb,$tc,$td)" => 4
      case _ => throw new Exception(s"Does not support getting the arity of the tuple `$tup`, ${tup.typ}.")
    }
  }

  def getTupleArity[T: IRType]: Int = {
    irTypeOf[T] match {
      case x if x == getTupleType(2) => 2
      case x if x == getTupleType(3) => 3
      case x if x == getTupleType(4) => 4
      case x => throw new Exception(s"Does not support getting the arity of the tuple type `$x`.")
    }
  }

  def isTupleType[T: IRType]: Boolean =
    try {
      getTupleArity[T] > 1
    } catch {
      case x: Exception => false
    }

  def projectTuple[C, R](tup:IR[R,C], idx:Int) = {
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

object RowLayoutTransformers {
  import RelationDSL.Predef._
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
            ir"$newList := ($newList.!).::(${constructTupleFromList(elems, s)}.asInstanceOf[$tupType])"
          case ir"val $listVar = $$list.!; $subBody: $tp2" =>
            val subBody2 = subBody rewrite {
              case ir"$$listVar.foreach[$t](x => $fbody)" => ir"($newList.!) foreach {e => val x = TupledRow(e.asInstanceOf[Product]).toRow; $fbody}"
            }
            subBody2 subs 'listVar -> {System.err.println(s"inside list var access $subBody"); throw RewriteAbort()}
//          case ir"$$list.!.foreach[$t]($f)" =>  ir"($newList.!) foreach $f"
        }
        val body1 = body0 subs 'list -> {System.err.println(s"list body $tupType::$body0"); throw RewriteAbort()}
        ir"val newList: Var[List[$tupType]] = Var(Nil); $body1"
      }}

    // TODO maybe move it to the core compilation facilities.
    implicit class IRCastingOps[T: IRType, C](e: IR[T, C]) {
      def __inlinedCast[T2: IRType]: IR[T2, C] = e.asInstanceOf[IR[T2, C]]
    }

    def hashMapRewrite[T:IRType,C](hm: IR[HashMap[String, Row],C{val hm: HashMap[String, Row]}], body: IR[T,C{val hm: HashMap[String, Row]}]): IR[T,C] = {
      var size: Int = -1
      body analyse {
        case ir"$$hm += (($_: String, TupledRow((($tup): scala.Product)).toRow))" =>
          size = getTupleArity(tup)
      }
      if (size == -1) {
        throw RewriteAbort()
      }
      getTupleType(size) match { case tupTypeVal: IRType[tupType] =>
        val newHm = ir"newHm? : HashMap[String, tupType]"
        val body0 = body rewrite {
          case ir"$$hm += (($key: String, TupledRow($tup: scala.Product).toRow)); ()" =>
            ir"$newHm += (($key, ($tup.asInstanceOf[tupType]))); ()"
          case ir"$$hm.contains($key)" => ir"$newHm.contains($key)"
          case ir"$$hm.apply($key: String)" => ir"TupledRow(${ir"$newHm.apply($key)".__inlinedCast[Product]}).toRow"
        }
        val body1 = body0 subs 'hm -> {throw RewriteAbort()}
        ir"val newHm: HashMap[String, tupType] = new HashMap[String, tupType]; $body1"
      }}


    rewrite {
      case ir"($r: Row).getValue($idx)" =>
        ir"TupledRow.fromRow($r).getElem($idx)"
      case ir"val $list: Var[List[Row]] = Var(Nil); $body: $t2" =>
        listRewrite(list, body)
      case ir"val $hm: HashMap[String, Row] = new HashMap[String, Row]; $body: $t2" =>
        hashMapRewrite(hm, body)
    }

  }

  object TupledRowFusion extends RelationDSL.SelfTransformer with SimpleRuleBasedTransformer with BottomUpTransformer with FixPointTransformer {
    rewrite {
      case ir"TupledRow.fromRow(($t: TupledRow).toRow)" =>
        t
      case ir"TupledRow($e1: Product).toRow.append(TupledRow($e2: Product).toRow)" =>
        val n1 = getTupleArity(e1)
        val n2 = getTupleArity(e2)
        val elems = constructTuple((i: Int) => if (i < n1) projectTuple(e1, i) else projectTuple(e2, i - n1), n1 + n2).asInstanceOf[IR[Product, e1.Ctx]]
        ir"TupledRow($elems).toRow"
    }

  }

  object TupledRowLowering extends RelationDSL.SelfTransformer with SimpleRuleBasedTransformer with BottomUpTransformer with FixPointTransformer {
    rewrite {
      case ir"TupledRow($e: Product).getElem(${Const(idx)})" =>
        projectTuple(e, idx)
      case ir"TupledRow($e: Product).toRow" =>
        val arity = getTupleArity(e)
        val elems = (0 until arity).map(i => projectTuple(e, i))
        ir"Row(List($elems*), ${Const(arity)})"
    }
  }
}


object ListToArrayBuffer extends RelationDSL.SelfTransformer with SimpleRuleBasedTransformer with BottomUpTransformer with FixPointTransformer {
  import RelationDSL.Predef._

  def listRewrite[T:IRType, R:IRType,C](list: IR[Var[List[R]],C{val list: Var[List[R]]}], body: IR[T,C{val list: Var[List[R]]}]): IR[T,C] = {
    val ab = ir"ab? : ArrayBuffer[R]"
    val body0 = body rewrite {
      case ir"$$list := ($$list.!).::($e: R)" =>
        ir"$ab += $e; ()"
      case ir"val $listVar = $$list.!; $subBody: $tp2" =>
        val subBody2 = subBody rewrite {
          case ir"$$listVar.foreach[$t](x => $fbody)" => ir"for(i <- 0 until $ab.length) { val x = $ab(i); $fbody}"
        }
        subBody2 subs 'listVar -> {System.err.println(s"inside list var access $subBody"); throw RewriteAbort()}
      //          case ir"$$list.!.foreach[$t]($f)" =>  ir"($newList.!) foreach $f"
    }
    val body1 = body0 subs 'list -> {System.err.println(s"list body $body0"); throw RewriteAbort()}
    ir"val ab = new ArrayBuffer[R](); $body1"
  }
  rewrite {
    case ir"val $list: Var[List[$t1]] = Var(Nil); $body: $t2" =>
      listRewrite(list, body)
  }
}

object ArrayBufferColumnar extends RelationDSL.SelfTransformer with SimpleRuleBasedTransformer with BottomUpTransformer with FixPointTransformer {
  import RelationDSL.Predef._
  import TupleProcessing._

  def arrayBufferRewrite[T:IRType, R:IRType,C](ab: IR[ArrayBuffer[R],C {val ab: ArrayBuffer[R]}], body: IR[T,C{val ab: ArrayBuffer[R]}]): IR[T,C] = {
    val abt = ir"abt? : (ArrayBuffer[String], ArrayBuffer[String])"
    val n = println(getTupleArity[R])
    val body0 = body rewrite {
      case ir"$$ab.length" =>
        ir"$abt._1.length"
      case ir"$$ab += ($e: R); ()" =>
        ir"$abt._1 += ${projectTuple(e, 0)}; $abt._2 += ${projectTuple(e, 1)}; ()"
      case ir"$$ab($i)" =>
        ir"($abt._1($i), $abt._2($i))".asInstanceOf[IR[R, C]]
    }
    val body1 = body0 subs 'ab -> {System.err.println(s"list body $body0"); throw RewriteAbort()}
    ir"val abt = (new ArrayBuffer[String](), new ArrayBuffer[String]()); $body1"
  }
  rewrite {
    case ir"val $ab: ArrayBuffer[$t1] = new ArrayBuffer[t1]; $body: $t2" if isTupleType(t1) =>
      arrayBufferRewrite(ab, body)
  }
}