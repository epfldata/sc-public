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
  
  val params = newTypeParams('A); import params._

  rewrite += statement {
    case sym -> (x @ dsl"new Array[A]($size)") => 
      val e = dsl"Mem.alloc[A]($size)"     
      arrays += Arr(e)
      e
  }

  case class Arr[T](arr: Rep[Array[T]])(implicit val tp: TypeRep[T])

  var arrays = mutable.ArrayBuffer[Arr[_]]()
  
  val arraySizes = mutable.Map[Rep[_], IR.Var[Int]]()
  
  //override def transformBlock[T: TypeRep](b: Block[T]) = { // doesn't work; FIXME remove this confusing one
  override def transformBlockTyped[T: TypeRep, S: TypeRep](b: Block[T]): to.Block[S] = {
    
    val oldArrays = arrays
    arrays = mutable.ArrayBuffer[Arr[_]]()
    
    val nb = reifyBlock {
      b.stmts foreach { s => 
        transformStm(s)
      }
      
      arrays foreach {
        case a => // Note: a => doesn't work (for very obscure Scala reasons)
          import a.tp
          dsl"Mem.free(${a.arr})"
      }
      
      apply(b.res)
    }
    arrays = oldArrays
    nb.asInstanceOf[Block[S]]
  }
  
  
}
