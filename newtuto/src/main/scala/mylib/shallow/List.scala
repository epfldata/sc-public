package mylib
package shallow

import ch.epfl.data.sc.pardis.annotations._

@deep
@quasi
@needs[Numeric[_] :: (_,_) :: Seq[_]]
@noImplementation // TODO why needed?
class List[A](val data: Seq[A]) { // TODO name it something else to avoid confusions with scala.List
  // FIXME: actually many of the functions are only *conditionally* pure
  
  @pure
  def map[B](f: A => B): List[B] =
    new List(data map f)
  
  @pure
  def filter(f: A => Boolean): List[A] =
    new List(data filter f)
  
  @pure
  def fold[B](init: B)(f: (B,A) => B): B =
    (init /: data)(f)
  
  @pure
  def size: Int = fold(0)((s, _) => s + 1)
  
  override def toString: String = s"List(${data mkString ", "})"
}

object List {
  //@pure // FIXME: java.lang.Exception: Unable to unlift type $tpe
  def apply[A](data: A*): List[A] = new List[A](data.toSeq)
  
  //@pure // FIXME
  def zip[A,B](as: List[A], bs: List[B]) = new List(as.data zip bs.data)
  
  //object empty extends List(Seq()) // FIXME: does not get deeply embedded
  val empty = List(Seq()) // FIXME: does not get deeply embedded
}


