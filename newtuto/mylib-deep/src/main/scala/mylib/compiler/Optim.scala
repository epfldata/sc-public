package mylib
package compiler

import ch.epfl.data.sc
import ch.epfl.data.sc.pardis.ast.lambda.Lambda
import ch.epfl.data.sc.pardis.deep.scalalib.NumericOps
import ch.epfl.data.sc.pardis.deep.scalalib.collection.SeqIRs.SeqApplyObject
import ch.epfl.data.sc.pardis.ir.ObjectOpsIRs.Rep
import ch.epfl.data.sc.pardis.ir.{PardisLambda, PardisLiftedSeq, PardisVarArg}
import ch.epfl.data.sc.pardis.quasi.TypeParameters._
import ch.epfl.data.sc.pardis.quasi.anf.{LambdaExtract, Lambda2Extract}
import mylib.deep.{ListComponent, MyLibDSL}

import shallow._

object Optim {
  
  class Offline(override val IR: MyLibDSL) extends sc.pardis.optimization.RecursiveRuleBasedTransformer[MyLibDSL](IR) {
    
    // Replacing size on singleton lists by literal 1
    rewrite += symRule {
      case dsl"List($_).size" => dsl"1"
    }
    
    // Optimizing chained map applications; you can check it works by commenting the one in `Online`
    rewrite += {
      val params = newTypeParams('A,'B,'C); import params._
      rule {
        case dsl"($ls: List[A]).map($f: A => B).map($g: B => C)" =>
          dsl"($ls).map(x => $g($f(x)))"
      }}
    
    // Reduction of redexes (inlining calls to lambdas)
    rewrite += {
      val params = newTypeParams('A,'B); import params._
      symRule {
        case dsl"(${Lambda(f)}: A => B).apply($arg)" =>
          f(arg)
      }}
    
  }
  
  trait Online extends ListComponent with NumericOps {
    
    private implicit val Context = Main.Context // QQ need an implicit context
    
//    // The map.map optimization as defined online: // doesn't work because of QQ pgrm (cf: Notes.md)
//    override def listMap[A: TypeRep, B: TypeRep](self : Rep[List[A]], f : Rep[((A) => B)]): Rep[List[B]] = self match {
//      case Def(self: ListMap[t1, A]) => self match {
//        case ListMap(self2, f2) =>
//          implicit val t1rep = self.typeA
//          val f2app = __app[t1, A](f2)
//          //ListMap[t1, B](self2, (x: Rep[t1]) => f(f2app(x)))
//          self2.map((x: Rep[t1]) => f(f2app(x)))
//      }
//      case _ => super.listMap(self, f)
//    }
    override def listMap[A: TypeRep, B: TypeRep](self : Rep[List[A]], f : Rep[((A) => B)]): Rep[List[B]] = {
      val params = newTypeParams('X); import params._
      self match {
        case dsl"($ls: List[X]).map($g: X => A)" => dsl"($ls).map(x => $f($g(x)))"
        case _ => super.listMap(self, f)
      }}
    
//    // Does not work:
//    override def listSize[A: TypeRep](self : Rep[List[A]]) : Rep[Int] = self match {
//      case Def(ListApplyObject(Def(PardisLiftedSeq(Seq())))) => unit(0)
//      case _ => super.listSize(self)
//    }
    
    // Another example of online transfo, using QQ:
    override def listSize[A: TypeRep](self : Rep[List[A]]) : Rep[Int] = self match {
        case dsl"shallow.List()" => dsl"0"
        case _ => super.listSize(self)
      }
    
  }
  
}

