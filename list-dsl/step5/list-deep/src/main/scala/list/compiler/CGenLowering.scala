package list
package compiler

import scala.collection.mutable.ArrayBuffer

import ch.epfl.data.sc.pardis
import pardis.optimization.RecursiveRuleBasedTransformer
import pardis.quasi.TypeParameters._

import list.deep.ListDSLOps
import list.shallow._  

class CGenLowering(override val IR: ListDSLOps) extends RecursiveRuleBasedTransformer[ListDSLOps](IR) {
  import scala.collection._
  
  implicit val ctx = IR // for quasiquotes
  
  import IR.Predef._
  
  val params = newTypeParams('A); import params._

  rewrite += statement {
    case sym -> dsl"new Array[A]($size)" =>
      val e = dsl"Mem.alloc[A]($size)"     
      arrays += Arr(e)
      e
  }

  case class Arr[T](arr: Rep[Array[T]])(implicit val tp: TypeRep[T])

  var arrays = mutable.ArrayBuffer[Arr[_]]()

  def postProcessBlock[T](b: Block[T]): Unit = {
    arrays foreach {
        case a =>
          import a._
          dsl"Mem.free($arr)"
      }
  }
  
  override def transformBlockTyped[T: TypeRep, S: TypeRep](b: Block[T]): to.Block[S] = {
    
    val oldArrays = arrays
    arrays = mutable.ArrayBuffer[Arr[_]]()
    
    val nb = reifyBlock {
      b.stmts foreach { s => 
        transformStm(s)
      }
      
      postProcessBlock(b)
      
      apply(b.res)
    }
    arrays = oldArrays
    nb.asInstanceOf[Block[S]]
  }
  
}
