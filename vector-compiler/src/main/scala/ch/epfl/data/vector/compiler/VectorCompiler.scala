package ch.epfl.data
package vector
package compiler

import pardis.types._
import pardis.ir._
import pardis.optimization._
import pardis.compiler._
import deep._
import prettyprinter._

class VectorCompiler(val DSL: VectorDSL) extends Compiler {
  pipeline += DCE



  val codegen = new VectorScalaGenerator

  // def compile[T](program: => Expression[T]): Unit = {
  //   implicit val typeT: PardisType[T] = program.tp
  //   val block = DSL.reifyBlock(program)
  //   val pipeline = new TransformerPipeline()
  //   pipeline += DCE
  //   val optimizedBlock = pipeline(DSL)(block)
  //   val irProgram = IRToProgram(DSL).createProgram(optimizedBlock)
  //   val codegen = new VectorScalaGenerator("GeneratedVectorApp")
  //   codegen.apply(irProgram)
  // }
}
