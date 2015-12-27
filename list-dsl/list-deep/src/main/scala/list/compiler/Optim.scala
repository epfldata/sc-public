package list
package compiler

import ch.epfl.data.sc.pardis
import pardis.deep.scalalib.NumericOps
import pardis.quasi.TypeParameters._
import pardis.optimization.RecursiveRuleBasedTransformer

import list.deep.{ListComponent, ListDSLOps}
import shallow._

object Optim {
  
  implicit val Context = Main.Context
  
  //object Offline {
    
    class HighLevel(override val IR: ListDSLOps) extends RecursiveRuleBasedTransformer[ListDSLOps](IR) {
      val params = newTypeParams('A,'B,'C); import params._
      import IR.Predef._
      
      /*
      // Replacing size on singleton lists by literal 1
      rewrite += symRule {
        case dsl"List($_).size" => dsl"1": Rep[_]
      }
      */
      
      // Replacing size on list constructors by a literal
      rewrite += symRule {
        case dsl"List($xs*).size" => unit(xs.size)   // dsl"${xs.size}" // doesn't work (why?)
      }
      
      // Optimizing chained map applications; you can check it works by commenting the one in `Online`
      rewrite += rule {
        case dsl"($ls: List[A]).map($f: A => B).map($g: B => C)" =>
          dsl"($ls).map(x => $g($f(x)))"
      }
      
      // Optimizing chained filter applications
      rewrite += rule {
        case dsl"($ls: List[A]).filter($f: A => Boolean).filter($g: A => Boolean)" =>
          dsl"($ls).filter(x => $f(x) && $g(x))"
      }
      
      // Note that optimizing chained map/filter needs a lowering, or to convert it to fold
      
    }
    
    class Generic(override val IR: ListDSLOps) extends RecursiveRuleBasedTransformer[ListDSLOps](IR) {
      val params = newTypeParams('A,'B); import params._
      
      import IR.Predef._
      
      // Reduction of redexes (inlining calls to lambdas)
      rewrite += symRule {
        case dsl"(${Lambda(f)}: A => B).apply($arg)" =>
          f(arg)
        
        /*
        // Another example of generic optimization:
        case dsl"(${Constant(a)}: Double) * (${Constant(b)}: Double)" =>
          dsl"${a*b}"
        */
      }
      
    }
  
  //}
  
  trait Online extends ListComponent with NumericOps {
    
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

