package list
package shallow

import ch.epfl.data.sc.pardis.annotations._

@deep
@needs[(_,_) :: Seq[_]]
class List[A](val data: Seq[A]) {
  
  @pure
  def map[B](f: A => B): List[B] =
    new List(data map f)
  
  @pure
  def filter(f: A => Boolean): List[A] =
    new List(data filter f)
  
  @pure
  def fold[B](init: B, f: (B,A) => B): B =
    (init /: data)(f)
  
  @pure
  def size: Int = fold[Int](0, (s, _) => s + 1)
  
  @pure
  def + (that: A): List[A] = new List(data :+ that)

  def print(): Unit = {
    printf("List: ")
    for(e <- data) {
      Predef.print(e + ", ")  
    }
    printf("Size: %d\n", size)
  }
  
  override def toString: String = s"List(${data mkString ", "})"
}

object List {
  def apply[A](data: A*): List[A] = new List[A](data.toSeq)
  
  def apply[A](): List[A] = new List[A](Seq())
  
  def zip[A,B](as: List[A], bs: List[B]) = new List(as.data zip bs.data)
  
  val empty = List(Seq())
}


