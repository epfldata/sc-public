package mylib
package compiler

import ch.epfl.data.sc
import ch.epfl.data.sc.pardis.deep.scalalib.NumericOps
import ch.epfl.data.sc.pardis.deep.scalalib.collection.SeqIRs.SeqApplyObject
import ch.epfl.data.sc.pardis.ir.{PardisLiftedSeq, PardisVarArg}
import ch.epfl.data.sc.pardis.quasi.TypeParameters._
import mylib.deep.{ListComponent, MyLibDSL}

import shallow._

object Optim {
  
  class Offline(override val IR: MyLibDSL) extends sc.pardis.optimization.RecursiveRuleBasedTransformer[MyLibDSL](IR) {
    
    rewrite += {
      val params = newTypeParams('A,'B,'C); import params._
      rule {
        case dsl"($ls: List[A]).map($f: A => B).map($g: B => C)" =>
          dsl"($ls).map(x => $g($f(x)))"
      }}
    
  }
  
  trait Online extends ListComponent with NumericOps {
    
    // The map.map optimization as defined online:
    override def listMap[A: TypeRep, B: TypeRep](self : Rep[List[A]], f : Rep[((A) => B)]): Rep[List[B]] = self match {
      case Def(self: ListMap[t1, A]) => self match {
        case ListMap(self2, f2) =>
          implicit val t1rep = self.typeA
          val f2app = __app[t1, A](f2)
          ListMap[t1, B](self2, (x: Rep[t1]) => f(f2app(x)))
      }
      case _ => super.listMap(self, f)
    }
    
//    // Does not work:
//    override def listSize[A: TypeRep](self : Rep[List[A]]) : Rep[Int] = self match {//ListSize[A](self)
//      case Def(ListApplyObject(Def(PardisLiftedSeq(Seq())))) => unit(0)
//      case _ => super.listSize(self)
//    }
    
    // Another example of online transfo, using QQ this time:
    override def listSize[A: TypeRep](self : Rep[List[A]]) : Rep[Int] = {
      implicit val Context = Main.Context // QQ need an implicit context
      self match {
        case dsl"shallow.List()" => dsl"0"
        case _ => super.listSize(self)
      }}
    
  }
  
}

