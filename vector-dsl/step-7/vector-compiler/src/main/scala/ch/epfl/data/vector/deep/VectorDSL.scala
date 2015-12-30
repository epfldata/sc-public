package ch.epfl.data
package vector
package deep

import sc.pardis.deep.scalalib._
import sc.pardis.types.PardisTypeImplicits._
import sc.pardis.deep.scalalib.collection._

trait VectorDSL extends VectorComponent with ScalaPredefOps with SeqOps with NumericOps

trait VectorDSLOpt extends VectorDSL with VectorOpt with IntPartialEvaluation

trait VectorDSLInline extends VectorDSL with VectorPartialEvaluation with VectorImplementations
