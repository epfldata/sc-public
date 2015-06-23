package ch.epfl.data
package vector
package deep

trait VectorOpt extends VectorPartialEvaluation {
  override def vector$plus(self: Rep[Vector], v2: Rep[Vector]): Rep[Vector] = (self, v2) match {
    case (Def(VectorZeroObject(_)), _) => v2
    case (_, Def(VectorZeroObject(_))) => self
    case _                             => super.vector$plus(self, v2)
  }
}
