package ch.epfl.data
package vector
package app

import compiler._

object Example3Shallow {
  import shallow._
  def main(args: Array[String]) {
    val v1 = Vector.zero(3)
    val v2 = Vector(Seq(2, 3, 4))
    val v3 = Vector(Seq(1, 3, 5))
    val res = v1 + v2 + v3
    val exp = Vector(Seq(4, 6, 8))
    println(res sameAs exp)
  }
}

object Example3Deep {
  import sc.pardis.types.PardisTypeImplicits._
  import deep._
  def main(args: Array[String]) {
    val context = new VectorDSLOpt {
      implicit def liftInt(i: Int): Rep[Int] = unit(i)
      def prog = {
        val v1 = Vector.zero(3)
        val v2 = Vector(Seq(2, 3, 4))
        val v3 = Vector(Seq(1, 3, 5))
        val res = v1 + v2 + v3
        val exp = Vector(Seq(4, 6, 8))
        println(res sameAs exp)
      }
    }
    import context._
    new VectorCompiler(context).compile(prog, "GeneratedVectorApp")
  }
}

object Example3Shadow {
  import sc.pardis.types.PardisTypeImplicits._
  import shallow._
  import shadow._
  def main(args: Array[String]) {
    val prog = dslOpt {
      val v1 = Vector.zero(3)
      val v2 = Vector(Seq(2, 3, 4))
      val v3 = Vector(Seq(1, 3, 5))
      val res = v1 + v2 + v3
      val exp = Vector(Seq(4, 6, 8))
      println(res sameAs exp)
    }
  }
}

object Example3DeepInline {
  import sc.pardis.types.PardisTypeImplicits._
  import deep._
  def main(args: Array[String]) {
    val context = new VectorDSLInline {
      implicit def liftInt(i: Int): Rep[Int] = unit(i)
      def prog = {
        val v1 = Vector.zero(3)
        val v2 = Vector(Seq(2, 3, 4))
        val v3 = Vector(Seq(1, 3, 5))
        val res = v1 + v2 + v3
        val exp = Vector(Seq(4, 6, 8))
        println(res sameAs exp)
      }
    }
    import context._
    new VectorCompiler(context).compile(prog, "GeneratedVectorApp")
  }
}

object Example3DeepOptInline {
  import sc.pardis.types.PardisTypeImplicits._
  import deep._
  def main(args: Array[String]) {
    val context = new VectorDSLOpt {
      implicit def liftInt(i: Int): Rep[Int] = unit(i)
      def prog = {
        val v1 = Vector.zero(3)
        val v2 = Vector(Seq(2, 3, 4))
        val v3 = Vector(Seq(1, 3, 5))
        val res = v1 + v2 + v3
        val exp = Vector(Seq(4, 6, 8))
        println(res sameAs exp)
      }
    }
    import context._
    new VectorCompilerOpt(context).compile(prog, "GeneratedVectorApp")
  }
}
