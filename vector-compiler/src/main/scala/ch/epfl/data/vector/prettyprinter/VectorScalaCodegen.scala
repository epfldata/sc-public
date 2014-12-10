package ch.epfl.data
package vector
package prettyprinter

import pardis.utils.Document
import pardis.ir._
import pardis.prettyprinter._
import scala.language.implicitConversions

class VectorScalaGenerator extends ScalaCodeGenerator {

  override def getHeader: Document = s"""package ch.epfl.data
package vector

import ch.epfl.data.vector.shallow._

"""

  override def getTraitSignature(): Document = s"""object GeneratedVectorApp {
  def main(args: Array[String]): Unit = """
}
