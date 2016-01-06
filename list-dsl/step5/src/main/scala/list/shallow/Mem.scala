package list.shallow

import ch.epfl.data.sc.pardis.annotations._
import scala.reflect.ClassTag

@deep
@needs[ClassTag[_]]
class Mem

object Mem {
  
  //def alloc[T](size: Int): Array[T] = ???
  //def alloc[T: ClassTag](size: Int): Array[T] = Array.ofDim[T](size)
  def alloc[T](size: Int): Array[T] = Array.ofDim[Any](size).asInstanceOf[Array[T]] // doesn't work with, eg, Int
  
  def free[T](obj: Array[T]): Unit = ()
  
}
