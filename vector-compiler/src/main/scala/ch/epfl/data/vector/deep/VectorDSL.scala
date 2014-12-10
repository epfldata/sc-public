package ch.epfl.data
package vector
package deep

import pardis.deep.scalalib._
import pardis.deep.scalalib.collection._

trait VectorDSL extends VectorComponent with ScalaPredef with SeqOps with IntOps

trait VectorDSLOpt extends VectorDSL with VectorOpt with IntPartialEvaluation
