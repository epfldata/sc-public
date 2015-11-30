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

    case dsl"shallow.List[A](.*$xs)" =>
      block {
        val buffer = dsl"new ArrayBuffer[A](${unit(xs.size)})"
        for (x <- xs) dsl"$buffer append $x"
        buffer
      }
      
    case dsl"shallow.List[A]()" =>
      dsl"new ArrayBuffer[A]()"
    
  }
  
  // Replacing map
  rewrite += symRule {
    
    case dsl"(${ArrFromLs(arr)}: List[A]).map($f: A => B)" =>
      dsl"""
        val r = new ArrayBuffer[B]()
        for (x <- $arr) r append $f(x)
        r
      """
      
    //// NOTE: this works ('fold' is in turn transformed by this recursive transformer)
    //case dsl"($ls: List[A]).map($f: A => B)" =>
    //  val code = dsl"$ls.fold(shallow.List[B](), (ls:List[B], e:A) => ls + $f(e))"
    //  code: Rep[_]
      
  }
  
  // Replace map/filter, filter, fold, zip and +
  rewrite += symRule {
    
    // problematic: will leave the 'map' effectful nodes (because 'map' is processed before)
    case dsl"(${ArrFromLs(arr)}: List[A]).map($f: A => B).filter($g: B => Boolean)" =>
      dsl"""
        val r = new ArrayBuffer[B]()
        for (x <- $arr) { val e = $f(x); if($g(e)) r append e }
        r
      """
    
    case dsl"(${ArrFromLs(arr)}: List[A]).filter($f: A => Boolean)" =>
      dsl"""
        val r = new ArrayBuffer[A]()
        for (x <- $arr) if($f(x)) r append x
        r
      """
      
    case dsl"(${ArrFromLs(arr)}: List[A]).fold[B]($init, $f)" =>
      block(dsl"""
         var acc = $init
         for (x <- $arr) acc = $f(acc,x)
         acc
      """)
      
    case dsl"(${ArrFromLs(arr)}: List[A]) + $x" =>
      block(dsl"""
        val r = new ArrayBuffer[A]($arr.size)
        for (x <- $arr) r append x
        r append $x
        r
      """)
    
    case dsl"shallow.List.zip[A,B] (${ArrFromLs(xs)}, ${ArrFromLs(ys)})" =>
      //  val n = $xs.size max $ys.size  // Int's mirror does not expose 'max'
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
  
  /** Use to reify blocks for more legibility of generated code */
  def block[T](x: => Rep[T]) = {
    //reifyBlock(x)
    x
  }
  
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











