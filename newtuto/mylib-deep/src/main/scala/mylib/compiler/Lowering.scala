package mylib
package compiler

import ch.epfl.data.sc
import ch.epfl.data.sc.pardis.ir.{PardisLiftedSeq}
import ch.epfl.data.sc.pardis.quasi.TypeParameters._
import mylib.deep.{ListComponent, MyLibDSL}
import ch.epfl.data.sc.pardis.types.PardisTypeImplicits._  

//import shallow._

class Lowering(override val IR: MyLibDSL) extends sc.pardis.optimization.RecursiveRuleBasedTransformer[MyLibDSL](IR) {
  
  val params = newTypeParams('A,'B,'C); import params._
  
  import IR._  
  
  // Replacing List construction
  rewrite += symRule {
    //case x: Rep[List[t]] if x.correspondingNodeOption exists (_.isInstanceOf[ListApplyObject[_]]) => x match { //implicit val _ = x.tp; x match {
    //  case IR.Def(app @ ListApplyObject(IR.Def(PardisLiftedSeq(xs)))) =>
    //    implicit val t = app.typeA
        //reifyBlock {
        
    case IR.Def(app @ ListApplyObject(IR.Def(PardisLiftedSeq(xs:Seq[Rep[t]])))) =>
      implicit val t = app.typeA.asInstanceOf[TypeRep[t]]
      //reifyBlock { ...  
      val buffer = dsl"new ArrayBuffer[t](${unit(xs.size)})"
      for (x <- xs) dsl"$buffer append $x"
      buffer : Rep[_]
    
  }
  
  // Replacing map
  rewrite += symRule {
    case dsl"($ls: List[A]).map($f: A => B)" =>
    //case dsl"($ls: ArrayBuffer[A]).map($f: A => B)" => // FIXME why doesn't work?
      val arr = ls.asInstanceOf[Rep[ArrayBuffer[A]]]  
      val r = dsl"""
        //val r = shallow.List[A]()
        val r = new ArrayBuffer[B]()
        for (x <- $arr) r append $f(x)
        r
      """
      r: Rep[_]
  }
  
}













