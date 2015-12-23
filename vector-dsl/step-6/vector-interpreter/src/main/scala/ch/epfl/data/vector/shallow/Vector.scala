package ch.epfl.data
package vector
package shallow

import ch.epfl.data.sc.pardis.annotations._

@deep
class Vector(val data: Seq[Int]) {
  @pure def +(v2: Vector): Vector = {
    val resultData = for (i <- 0 until data.size) yield (data(i) + v2.data(i))
    Vector(resultData.toSeq)
  }

  @pure def *(v2: Vector): Int = {
    var sum = 0
    for (i <- 0 until data.size) {
      sum += data(i) * v2.data(i)
    }
    sum
  }

  @pure def sameAs(v2: Vector): Boolean = {
    var result = true
    for (i <- 0 until data.size) {
      if (data(i) != v2.data(i))
        result = false
    }
    result
  }

  override def toString: String = s"Vector(${data.mkString(", ")})"
}

object Vector {
  @pure def zero(n: Int): Vector = new Vector(0.until(n).map(x => 0).toSeq)
  @pure def apply(data: Seq[Int]): Vector = new Vector(data)
}
