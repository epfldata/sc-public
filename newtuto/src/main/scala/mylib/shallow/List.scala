package mylib
package shallow

import ch.epfl.data.sc.pardis.annotations._

@deep
@deepExt
@needs[Seq[_]]
case class List[A](data: A*) {
  
  def map[B](f: A => B) = List(data map f)
  
  def size: Int = data.size
  
}


