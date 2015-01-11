package ch.epfl.data
package vector
package prettyprinter

import pardis.utils.Document
import pardis.ir._
import pardis.prettyprinter._
import scala.language.implicitConversions
import deep.VectorDSL

class VectorScalaGenerator(val IR: VectorDSL) extends ScalaCodeGenerator with ASTCodeGenerator[VectorDSL] {

  override def getHeader: Document = s"""package ch.epfl.data
package vector

import ch.epfl.data.vector.shallow._

"""

  override def getTraitSignature(): Document = s"""object GeneratedVectorApp {
  def main(args: Array[String]): Unit = """
}
