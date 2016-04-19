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

class RelationCompiler(val DSL: RelationDSLOpsPackaged) extends Compiler[RelationDSLOpsPackaged] {
  
  // Pipeline Definition:
  pipeline += DCE

  val schemaAnalysis = new SchemaAnalysis(DSL)

  pipeline += schemaAnalysis
  pipeline += new RecordsLowering(DSL, schemaAnalysis)
  //pipeline += new ColumnStoreLowering(DSL, schemaAnalysis)

  pipeline += DCE
  
  
  // Outputting Scala code inside an executable wrapper:
  
  val codeGenerator = 
      new ScalaCodeGenerator with ASTCodeGenerator[RelationDSLOpsPackaged] with ArrayScalaCodeGen {
        val IR = DSL
        override def header(): Document = s"""
          |package relation
          |import relation.shallow._""".stripMargin
        override def getTraitSignature(): Document = s"""
          |object $outputFileName {
          |  def main(args: Array[String]): Unit = Timing.time(""".stripMargin
        override def footer(): Document = s"""
          |, "Query Execution")}
          |""".stripMargin
      }

  val outputFileName: String = "GenApp"
  
  def compile(program: => DSL.Rep[Unit]): Unit = {
    import DSL.Predef._
    val folder = "src/main/scala"
    
    // Create the directories if it does not already exist
    new java.io.File(s"generator-out/$folder").mkdirs()
    
    try compile(program, s"$folder/$outputFileName")
    catch {
      case LoweringException(msg) =>
        System.err.println("Error during lowering: "+msg)
    }
  }
}
