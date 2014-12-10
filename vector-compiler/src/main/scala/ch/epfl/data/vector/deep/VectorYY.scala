package ch.epfl.data
package vector
package deep

import ch.epfl.yinyang.api._

trait VectorYY extends VectorDSL with BaseYinYang with FullyUnstaged with Stager {
  implicit object LiftInt extends LiftEvidence[scala.Int, Rep[Int]] {
    def lift(v: scala.Int): Rep[Int] = unit(v)
    def hole(tpe: TypeRep[scala.Int], symbolId: scala.Int): Rep[Int] = ???
  }

  def main(): Any

  override def stage[T](): T = {
    lazy val res = main().asInstanceOf[Rep[T]]
    new compiler.VectorCompiler(this).compile(res, "GeneratedVectorApp")(null)
    res.asInstanceOf[T]
  }
}
