package relation
package compiler

import ch.epfl.data.sc.pardis
import pardis.deep.scalalib.ArrayScalaCodeGen
import pardis.optimization._
import pardis.compiler._
import pardis.ir._
import pardis.utils.document._
import pardis.prettyprinter._
import deep._

class RelationCompiler(val DSL: RelationDSLOpsPackaged, name: String) extends Compiler[RelationDSLOpsPackaged] {
  
  // Pipeline Definition:
  pipeline += DCE

  val schemaAnalysis = new SchemaAnalysis(DSL)

  pipeline += schemaAnalysis
  pipeline += new RelationRecordLowering(DSL, schemaAnalysis)
  //pipeline += new RelationColumnStoreLowering(DSL, schemaAnalysis)

  pipeline += DCE
  
  
  // Outputting Scala code inside an executable wrapper:
  
  val codeGenerator = 
      new ScalaCodeGenerator with ASTCodeGenerator[RelationDSLOpsPackaged] with ArrayScalaCodeGen {
        val IR = DSL
        override def header(): Document = s"""
          |package relation
          |import relation.shallow._""".stripMargin
        override def getTraitSignature(): Document = s"""
          |object $name {
          |  def main(args: Array[String]): Unit = """.stripMargin
        override def footer(): Document = s"""
          |}
          |""".stripMargin
      }
  
}
