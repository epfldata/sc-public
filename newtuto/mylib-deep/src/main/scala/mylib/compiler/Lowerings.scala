package mylib
package compiler

import scala.collection.mutable.ArrayBuffer

import ch.epfl.data.sc
import sc.pardis.optimization.RecursiveRuleBasedTransformer
import sc.pardis.quasi.TypeParameters._

import mylib.deep.MyLibDSLOps
import mylib.shallow._  


class Lowering(override val IR: MyLibDSLOps) extends RecursiveRuleBasedTransformer[MyLibDSLOps](IR) {
  
  implicit val ctx = IR // for quasiquotes
  
  val params = newTypeParams('A,'B,'C); import params._
  import IR.Predef._
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  /*
  // Required to tell SC what type tranformations are being performed
  override def transformType[T](implicit tp: TypeRep[T]): TypeRep[Any] = tp match {
    case lst: IR.ListType[t] => IR.ArrayBufferType[t](lst.typeA)
        .asInstanceOf[TypeRep[Any]]
    case _ => super.transformType(tp)
  }
  
  /** Our transformations transform Lists into ArrayBuffers, thus it is safe to view objects typed as Lists as ArrayBuffers.
    * Notice that since this extractor will be nested, we cannot use the same automatic type parameters.
    */
  object ArrFromLs {
    val params = newTypeParams('X); import params._
    
    def unapply[T](x: Rep[List[T]]): Option[Rep[ArrayBuffer[T]]] = x match {
      case dsl"$ls: List[X]" =>
        Some(ls.asInstanceOf[Rep[ArrayBuffer[T]]])
      case _ => None
    }
  }
  */
  
}














