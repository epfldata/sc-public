package relation.deep

import ch.epfl.data.sc.pardis
import pardis.ir._
import pardis.types.PardisTypeImplicits._
import pardis.effects._
import pardis.deep._
import pardis.deep.scalalib._
import pardis.deep.scalalib.collection._
import pardis.deep.scalalib.io._

import ch.epfl.data.sc.pardis.quasi.anf.{ BaseExt, BaseExtIR }
import ch.epfl.data.sc.pardis.quasi.TypeParameters.MaybeParamTag

trait RelationDSLExtraOps extends ArrayPartialEvaluation with RangePartialEvaluation with RichIntOps with InlineFunctions { selfObj =>
  implicit class RelationDSLExtraRep[A](self : Rep[Array[A]])(implicit typeA : TypeRep[A]) {
    def foreach[U: TypeRep](f: Rep[A => U]): Rep[Unit] = unit(0).until(self.length).foreach(__lambda { i=>
      val e = self(i)
      __app(f).apply(e)
    })
  }

  override def rangeForeach[U](self: Rep[Range], f: Rep[((Int) => U)])(implicit typeU: TypeRep[U]): Rep[Unit] =  selfObj.__for(self.start, self.end, self.step, (i: Rep[Int]) => __app(f).apply(i).asInstanceOf[Rep[Unit]])

  override def rangeApplyObject(start: Rep[Int], end: Rep[Int]): Rep[Range] =__newRange(start, end, unit(1))

  override def arrayLength[T](self: Rep[Array[T]])(implicit typeT: TypeRep[T]): Rep[Int] = array_Field__length(self)
}

object RelationDSLExtraIRs

trait RelationDSLExtraExtOps 

object RelationDSLExtraQuasiNodes
