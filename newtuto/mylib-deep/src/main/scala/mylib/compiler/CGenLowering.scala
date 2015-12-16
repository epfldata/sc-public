package mylib
package compiler

import scala.collection.mutable.ArrayBuffer

import ch.epfl.data.sc
import sc.pardis.optimization.RecursiveRuleBasedTransformer
import sc.pardis.quasi.TypeParameters._

import mylib.deep.MyLibDSLOps
import mylib.shallow._  

class CGenLowering(override val IR: MyLibDSLOps) extends RecursiveRuleBasedTransformer[MyLibDSLOps](IR) {
  import scala.collection._
  
  implicit val ctx = IR // for quasiquotes
  
  import IR.Predef._
  import IR.{ Block, __newVar }  // TODO put in Predef
  
  val params = newTypeParams('A,'B); import params._
  
  //rewrite += rule { case b @ Block(sts, r) => ... } // Doesn't work
  rewrite += symRule {
    
    case dsl"($arr: ArrayBuffer[A]) append $e" =>
      assert(subst isDefinedAt arr)
      val myarr = subst(arr).asInstanceOf[Rep[Array[A]]]
      val v = arraySizes(myarr)
      dsl"$myarr($v) = $e; $v = $v + 1"

    case dsl"($arr: ArrayBuffer[A]).size" =>
      assert(subst isDefinedAt arr)
      dsl"${arraySizes(subst(arr))}"
      
    case dsl"($arr: ArrayBuffer[A])($i)" =>
      val myarr = subst(arr).asInstanceOf[Rep[Array[A]]]
      dsl"$myarr($i)"
      
  }
  
  val arraySizes = mutable.Map[Rep[_], IR.Var[Int]]()
  
  //override def transformBlock[T: TypeRep](b: Block[T]) = { // doesn't work; FIXME remove this confusing one
  override def transformBlockTyped[T: TypeRep, S: TypeRep](b: Block[T]): to.Block[S] = {
    
    case class Arr[T](arr: Rep[Array[T]])(implicit val tp: TypeRep[T])
    val arrays = mutable.ArrayBuffer[Arr[_]]()
    
    val nb = reifyBlock {
      b.stmts foreach {
        // Note: does NOT handle dsl"new ArrayBuffer[A]()" -- when size isn't knozn, the lowering is ill-defined
        case Stm(sym, x @ dsl"new ArrayBuffer[A]($size)") =>
          val e = dsl"Mem.alloc[A]($size)"
          
          val sizeVar = __newVar(unit(0))
          arraySizes += (e -> sizeVar)
          
          arrays += Arr(e)
          subst += sym -> e
          
        case s => transformStm(s)
      }
      
      arrays foreach {
        case a => // Note: a => doesn't work (for very obscure Scala reasons)
          import a.tp
          dsl"Mem.free(${a.arr})"
      }
      
      apply(b.res)
    }
    
    nb.asInstanceOf[Block[S]]
  }
  
  
}







