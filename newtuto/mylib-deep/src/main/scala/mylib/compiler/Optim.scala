package mylib
package compiler

import ch.epfl.data.sc.pardis  
import pardis.deep.scalalib.NumericOps
import pardis.quasi.TypeParameters._
import pardis.optimization.RecursiveRuleBasedTransformer

import mylib.deep.MyLibDSLOps
import shallow._

class Optim (override val IR: MyLibDSLOps) extends RecursiveRuleBasedTransformer[MyLibDSLOps](IR) {
  
  implicit val Context = Main.Context
  
  val params = newTypeParams('A,'B,'C); import params._
  import IR.Predef._
  
  
  
  
  
  
  
}





