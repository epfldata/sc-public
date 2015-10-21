package mylib
package shallow

import ch.epfl.data.sc.pardis.annotations._

@deep
@quasi
@needs[(Char, Tuple2[_,_])] // FIXME: stupid
@noImplementation // TODO why needed?
class List[A](val data: Seq[A]) { // TODO name it something else to avoid confusions with scala.List
  
  def map[B](f: A => B): List[B] =
    new List(data map f)
  
  def filter(f: A => Boolean): List[A] =
    new List(data filter f)
  
  def fold[B](init: B)(f: (B,A) => B): B =
    (init /: data)(f)
  
  def size: Int = fold(0)((s, _) => s + 1)
  
  override def toString: String = s"List(${data mkString ", "})"
}
object List {
  def apply[A](data: A*): List[A] = new List[A](data.toSeq)
  def zip[A,B](as: List[A], bs: List[B]) = new List(as.data zip bs.data)
  
  //object empty extends List(Seq()) // FIXME: does not get deeply embedded
  val empty = List(Seq()) // FIXME: does not get deeply embedded
}


