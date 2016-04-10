package relation
package compiler

import ch.epfl.data.sc.pardis
import pardis.optimization._
import pardis.compiler._
import pardis.ir._
import pardis.deep.scalalib.ScalaCoreCCodeGen
import deep._

class MyCompiler(val DSL: RelationDSLOpsPackaged, name: String, offlineOptim: Boolean = false, lowering: Int = 0, cCodeGen: Boolean = false) extends Compiler[RelationDSLOpsPackaged] {
  
  // Pipeline Definition:
  
  pipeline += DCE

  val schemaAnalysis = new SchemaAnalysis(DSL)

  pipeline += schemaAnalysis
  pipeline += new RelationRecordLowering(DSL, schemaAnalysis)
  // pipeline += new RelationColumnStoreLowering(DSL, schemaAnalysis)

  pipeline += DCE
  
  if(cCodeGen) {
    require(lowering > 2)
    pipeline += ScalaCoreToC
  }
  
  
  // Outputting Scala and C code inside an executable wrapper:
  
  import pardis.prettyprinter._
  
  val codeGenerator = 
    if(!cCodeGen) {
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
    } else {
      new ScalaCoreCCodeGen with CASTCodeGenerator[RelationDSLOpsPackaged] {
        val IR = DSL
        import IR.Predef._
        import pardis.utils.document._
        import pardis.ir._
        import pardis.types._

        override def pardisTypeToString[A](t: PardisType[A]): String = 
          if (t.isArray)
            pardisTypeToString(t.typeArguments(0)) + "*"
          else
            super.pardisTypeToString(t)
      }
    }
  
}
