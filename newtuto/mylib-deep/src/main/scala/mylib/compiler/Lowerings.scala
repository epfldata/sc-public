package mylib
package compiler

import ch.epfl.data.sc
import ch.epfl.data.sc.pardis.ir.{PardisLiftedSeq}
import ch.epfl.data.sc.pardis.quasi.TypeParameters._
import mylib.deep.{ListComponent, MyLibDSL}
import ch.epfl.data.sc.pardis.types.PardisTypeImplicits._

import sc.pardis.shallow.scalalib

class ListLowering(override val IR: MyLibDSL) extends sc.pardis.optimization.RecursiveRuleBasedTransformer[MyLibDSL](IR) {
  
  val params = newTypeParams('A,'B,'C); import params._
  
  import IR._  
  
  override def transformType[T](implicit tp: TypeRep[T]): TypeRep[Any] = tp match {
    case lst: ListType[t] => ArrayBufferType[t](lst.typeA)
        .asInstanceOf[TypeRep[Any]]
    case _ => super.transformType(tp)
  }
  
  // Replacing List construction
  rewrite += symRule {

    //case dsl"shallow.List[A](..$args)" => // TODO implement syntax
    case dsl"shallow.List[A](${IR.Def(PardisLiftedSeq( xs ))}:_*)" =>
      //reifyBlock {  // to put the statements in a block -- not necessary here
      val buffer = dsl"new ArrayBuffer[A](${unit(xs.size)})"
      for (x <- xs) dsl"$buffer append $x"
      buffer : Rep[_]
      
    case dsl"shallow.List[A]()" =>
      dsl"new ArrayBuffer[A]()" : Rep[_]
    
  }
  
  //// Inlines size's implementation -- DOES NOT WORK
  //rewrite += symRule {
  //  case dsl"($ls: List[A]).size" =>
  //    listSize(ls)
  //}
  
  // Replacing map
  rewrite += symRule {
    
    //// The following WON'T work:
    //case dsl"($ls: List[A]).map[B]($f)" =>
    //case dsl"($ls: ArrayBuffer[A]).map($f: A => B)" =>
    //  val arr = ls.asInstanceOf[Rep[ArrayBuffer[A]]] 
    
    case dsl"(${ArrFromLs(arr)}: List[A]).map($f: A => B)" =>
      val code = dsl"""
        val r = new ArrayBuffer[B]()
        for (x <- $arr) r append $f(x)
        r
      """
      code: Rep[_]
      
    //// NOTE: this does NOT work
    //case dsl"($ls: List[A]).map($f: A => B)" =>
    //  val code = dsl"$ls.fold(shallow.List[B](), (ls:List[B], e:A) => ls + $f(e))"
    //  code: Rep[_]
      
  }
  
  // Replace map/filter, filter, fold, zip and +
  rewrite += symRule {
    
    case dsl"(${ArrFromLs(arr)}: List[A]).map($f: A => B).filter($g: B => Boolean)" =>
      val code = dsl"""
        val r = new ArrayBuffer[B]()
        for (x <- $arr) { val e = $f(x); if($g(e)) r append e }
        r
      """
      code: Rep[_]
    
    case dsl"(${ArrFromLs(arr)}: List[A]).filter($f: A => Boolean)" =>
      val code = dsl"""
        val r = new ArrayBuffer[A]()
        for (x <- $arr) if($f(x)) r append x
        r
      """
      code: Rep[_]
      
    case dsl"(${ArrFromLs(arr)}: List[A]).fold[B]($init, $f)" =>
      val code = dsl"""
         val r = new ArrayBuffer[A]()
         var acc = $init
         for (x <- $arr) acc = $f(acc,x)
         r
      """
      code: Rep[_]
      
    //// Unnecessary:
    //case dsl"(${ArrFromLs(arr)}: List[A]).size" =>
    //  dsl"$arr.size": Rep[_]
    
    //case dsl"(${ArrFromLs(arr)}: List[A]).as($ls) + ($x)" =>
    case dsl"(${ArrFromLs(arr)}: List[A]) + ($x)" =>
      val code = dsl"""
        val r = new ArrayBuffer[A]($arr.size)
        for (x <- $arr) r append x
        r append $x
        r
      """
      code: Rep[_]
    
    case dsl"shallow.List.zip[A,B] (${ArrFromLs(xs)}, ${ArrFromLs(ys)})" =>
      //  val n = $xs.size max $ys.size  // Int's mirror does not expose Size
      val code = dsl"""
        val n = ${max(xs.size, ys.size)}: Int
        val r = new ArrayBuffer[(A,B)](n)
        for (i <- scala.Range(0, n)) r append ( ($xs(i), $ys(i)) )
        r
      """
      code: Rep[_]
      
  }
  //}
  
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
  
  def max(a: Rep[Int], b: Rep[Int]): Rep[Int] = dsl"if ($a > $b) $a else $b"
  
}

class ArrBufLowering(override val IR: MyLibDSL) extends sc.pardis.optimization.RecursiveRuleBasedTransformer[MyLibDSL](IR) {
  
  val params = newTypeParams('A,'B,'C); import params._
  
  import IR._
  
  
  // Replacing foreach
  rewrite += symRule {
    case dsl"($arr: ArrayBuffer[A]).foreach($f)" =>
      val code = dsl"""
        var i = 0
        while (i < $arr.size) {
          $f($arr(i))
          i += 1
        }
      """
      code: Rep[_]
  }
  
  

}











