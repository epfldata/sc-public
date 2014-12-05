package ch.epfl.data
package vector
package shallow

import ch.epfl.data.pardis.annotations._

@deep
@noImplementation
class Vector(val data: Seq[Int]) {
  @pure def +(v2: Vector): Vector = {
    val resultData = data.zip(v2.data).map(x => x._1 + x._2)
    Vector(resultData)
  }

  @pure def *(v2: Vector): Int = {
    data.zip(v2.data).map(x => x._1 * x._2).sum
  }

  override def toString: String = s"Vector(${data.mkString(", ")})"
}

object Vector {
  @pure def zero(n: Int): Vector = new Vector(0.until(n).map(x => 0).toSeq)
  @pure def apply(data: Seq[Int]): Vector = new Vector(data)
}
