package list
package compiler

import ch.epfl.data.sc.pardis
import pardis.deep.scalalib.NumericOps
import pardis.quasi.TypeParameters._
import pardis.optimization.RecursiveRuleBasedTransformer

import list.deep.{ ListOps, ListDSLOps }
import shallow._

object Optim {
  
  implicit val Context = Main.Context
  
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
      case dsl"List($xs*).size" => dsl"${xs.size}"
      case dsl"List().size" => dsl"0"
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
  
  trait ListExpOptimizations extends ListOps {
    
    override def listMap[A: TypeRep, B: TypeRep](self : Rep[List[A]], f : Rep[((A) => B)]): Rep[List[B]] = {
      val params = newTypeParams('X); import params._
      self match {
        case dsl"($ls: List[X]).map($g: X => A)" => dsl"($ls).map(x => $f($g(x)))"
        case _ => super.listMap(self, f)
      }}
    
    // Another example of online transfo, using QQ:
    override def listSize[A: TypeRep](self : Rep[List[A]]) : Rep[Int] = self match {
        case dsl"shallow.List()" => dsl"0"
        case _ => super.listSize(self)
      }
    
  }
  
}

