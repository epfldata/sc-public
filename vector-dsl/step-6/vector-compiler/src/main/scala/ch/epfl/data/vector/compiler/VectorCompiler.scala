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
