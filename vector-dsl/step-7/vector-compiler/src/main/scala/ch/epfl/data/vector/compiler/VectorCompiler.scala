package ch.epfl.data
package vector
package compiler

import sc.pardis.types._
import sc.pardis.ir._
import sc.pardis.optimization._
import sc.pardis.compiler._
import deep._
import prettyprinter._

class VectorCompiler(val DSL: VectorDSL) extends Compiler[VectorDSL] {
  pipeline += DCE

  val codeGenerator = new VectorScalaGenerator(DSL)
}

object VectorTransformation extends TransformerHandler {
  def apply[Lang <: Base, T: PardisType](context: Lang)(block: context.Block[T]): context.Block[T] = {
    new VectorTransformation(context.asInstanceOf[VectorDSL]).optimize(block)
  }
}

class VectorCompilerOpt(val DSL: VectorDSL) extends Compiler[VectorDSL] {
  pipeline += DCE
  pipeline += VectorTransformation
  pipeline += ParameterPromotion
  pipeline += PartiallyEvaluate
  pipeline += DCE

  val codeGenerator = new VectorScalaGenerator(DSL)
}
