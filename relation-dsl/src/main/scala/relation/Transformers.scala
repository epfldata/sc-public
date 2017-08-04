package relation

import squid.ir._
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
        newBody subs 's -> Abort()

    }
  }

  object StreamLowering extends RelationDSL.TransformerWrapper(StreamInliner, StreamCtorInliner) with BottomUpTransformer with FixPointTransformer
}

