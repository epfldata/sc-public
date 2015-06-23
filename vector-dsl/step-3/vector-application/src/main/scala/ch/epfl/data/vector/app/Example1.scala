package ch.epfl.data
package vector
package app

import compiler.VectorCompiler

object Example1Shallow {
  import shallow._
  def main(args: Array[String]) {
    val v1 = Vector(Seq(1, 2, 3))
    val v2 = Vector(Seq(2, 3, 4))
    println(v1 + v2)
  }
}

object Example1Deep {
  import sc.pardis.types.PardisTypeImplicits._
  import deep._
  def main(args: Array[String]) {
    val context = new VectorDSL {
      implicit def liftInt(i: Int): Rep[Int] = unit(i)
      def prog = {
        val v1 = Vector(Seq(1, 2, 3))
        val v2 = Vector(Seq(2, 3, 4))
        println(v1 + v2)
      }
    }
    import context._

    new VectorCompiler(context).compile(prog, "GeneratedVectorApp")
    // the generated code is written to file
    // sc-examples/generator-out/GeneratedVectorApp.scala
  }
}
