package list
package compiler

import scala.collection.mutable.ArrayBuffer

import ch.epfl.data.sc.pardis
import pardis.optimization.RecursiveRuleBasedTransformer
import pardis.quasi.TypeParameters._

import list.deep.ListDSLOps
import list.shallow._  


class ListLowering(override val IR: ListDSLOps) extends RecursiveRuleBasedTransformer[ListDSLOps](IR) {
  
  implicit val ctx = IR // for quasiquotes
  
  val params = newTypeParams('A,'B,'C); import params._
  
  import IR.Predef._
  
  // Required to tell SC what type tranformations are being performed
  override def transformType[T](implicit tp: TypeRep[T]): TypeRep[Any] = tp match {
    case lst: IR.ListType[t] => IR.ArrayBufferType[t](lst.typeA).asInstanceOf[TypeRep[Any]]
    case _ => super.transformType(tp)
  }
  
  // Replacing List construction
  rewrite += symRule {
    
    case dsl"List[A]($xs*)" =>
      block {
        val buffer = dsl"new ArrayBuffer[A](${unit(xs.size)})"
        for (x <- xs) dsl"$buffer append $x"
        buffer
      }
      
    case dsl"List[A]()" =>
      dsl"new ArrayBuffer[A](0)"
    
  }
  
  // Replacing map
  rewrite += symRule {
    
    case dsl"(${ArrFromLs(arr)}: List[A]).map($f: A => B)" =>
      dsl"""
        val r = new ArrayBuffer[B]($arr.size)
        for (x <- $arr) r append $f(x)
        r
      """
    
  }
  
  // Replace map/filter, filter, fold, zip and +
  rewrite += symRule {
    
    case dsl"(${ArrFromLs(arr)}: List[A]).map($f: A => B).filter($g: B => Boolean)" =>
      dsl"""
        val r = new ArrayBuffer[B]($arr.size)
        for (x <- $arr) { val e = $f(x); if($g(e)) r append e }
        r
      """
    
    case dsl"(${ArrFromLs(arr)}: List[A]).filter($f: A => Boolean)" =>
      dsl"""
        val r = new ArrayBuffer[A]($arr.size)
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
      
    case dsl"(${ArrFromLs(arr)}: List[A]).size" =>
      dsl"$arr.size"

    case dsl"(${ArrFromLs(arr)}: List[A]).print" =>
      val tp = typeRep[A]
      val format =
        if (tp == typeRep[Int]) "%d, " else
        if (tp == typeRep[Double]) "%f, " else
        throw new Exception(s"Doesn't know how to print the type $tp")
      dsl"""
      printf("List: ")
      for (x <- $arr) printf($format, x)
      printf("Size: %d\n", $arr.size)
      """
      
    case dsl"List.zip[A,B] (${ArrFromLs(xs)}, ${ArrFromLs(ys)})" =>
      dsl"""
        val n: Int = ${max( dsl"$xs.size", dsl"$ys.size" )}
        val r = new ArrayBuffer[(A,B)](n)
        for (i <- Range(0, n)) r append ( ($xs(i), $ys(i)) )
        r
      """
      
  }
  
  /** Our transformations transform Lists into ArrayBuffers, thus it is safe to view objects typed as Lists as ArrayBuffers.
    * Notice that since this extractor will be nested, we cannot use the same automatic type parameters.
    */
  object ArrFromLs {
    def unapply[T](x: Rep[List[T]]): Option[Rep[ArrayBuffer[T]]] = x match {
      case dsl"$ls: List[T]" =>
        Some(ls.asInstanceOf[Rep[ArrayBuffer[T]]])
      case _ => None
    }
  }
  
  def max(a: Rep[Int], b: Rep[Int]): Rep[Int] = dsl"if ($a > $b) $a else $b"
  
  /** Use to reify blocks for more legibility of generated code */
  //def block[T: TypeRep](x: => Rep[T]) = {
  def block[T](x: => Rep[T]) = {
    //reifyBlock(x)
    x
  }
  
}


class ArrBufLowering(override val IR: ListDSLOps) extends RecursiveRuleBasedTransformer[ListDSLOps](IR) {
  
  implicit val ctx = IR // for quasiquotes
  
  val params = newTypeParams('A,'B,'C); import params._
  
  import IR.Predef._
  
  
  // Replacing foreach
  rewrite += symRule {
    case dsl"($arr: ArrayBuffer[A]) foreach $f" =>
      dsl"""
        var i = 0
        while (i < $arr.size) {
          $f($arr(i))
          i += 1
        }
      """
  }
  
  

}




class ArrayBufferToArray(override val IR: ListDSLOps) extends RecursiveRuleBasedTransformer[ListDSLOps](IR) {
  import scala.collection._
  
  implicit val ctx = IR // for quasiquotes
  
  import IR.Predef._
  
  val params = newTypeParams('A); import params._
  
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

  rewrite += statement {
    case sym -> (x @ dsl"new ArrayBuffer[A]($size)") => 
      val res = dsl"new Array[A]($size)"
      arraySizes(res) = newVar(unit(0))
      res
  }
  
  val arraySizes = mutable.Map[Rep[_], IR.Var[Int]]()
  
}












