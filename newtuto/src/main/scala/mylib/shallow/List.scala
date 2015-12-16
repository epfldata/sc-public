package mylib
package shallow

import ch.epfl.data.sc.pardis.annotations._

case class List[A](data: Seq[A]) {
  
  def map[B](f: A => B) = List(data map f)
  
  def size: Int = data.size
  
}


