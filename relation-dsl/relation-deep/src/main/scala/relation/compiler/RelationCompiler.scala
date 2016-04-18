package relation
package compiler

import ch.epfl.data.sc.pardis
import ch.epfl.data.sc.pardis.deep.scalalib.ArrayIRs.Array_Field__length
import pardis.optimization._
import pardis.compiler._
import pardis.ir._
import pardis.deep.scalalib.ScalaCoreCCodeGen
import deep._

class RelationCompiler(val DSL: RelationDSLOpsPackaged, name: String) extends Compiler[RelationDSLOpsPackaged] {
  
  // Pipeline Definition:
  
  pipeline += DCE

  val schemaAnalysis = new SchemaAnalysis(DSL)

  pipeline += schemaAnalysis
  pipeline += new RecordsLowering(DSL, schemaAnalysis)
  //pipeline += new ColumnStoreLowering(DSL, schemaAnalysis)

  pipeline += DCE
  
  
  // Outputting Scala code inside an executable wrapper:
  
  import pardis.prettyprinter._
  
  val codeGenerator = 
      new ScalaCodeGenerator with ASTCodeGenerator[RelationDSLOpsPackaged] {
        val IR = DSL
        import pardis.utils.document._
          override def nodeToDocument(node: PardisNode[_]): Document = {
    node match {
      case PardisFor(start, end, step, variable, block)                    => 
        val stepDoc = step match {
          case Constant(1) => doc""
          case _ => doc" by $step"
        }
        doc"for($variable <- $start until $end$stepDoc) ${blockToDocument(block)}"
      case Array_Field__length(self)                    => 
        doc"$self.length"
      case _ => super.nodeToDocument(node)
    }
  }
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
