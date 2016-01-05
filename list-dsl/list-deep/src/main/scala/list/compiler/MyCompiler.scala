package list
package compiler

import ch.epfl.data.sc.pardis
import pardis.optimization._
import pardis.compiler._
import pardis.deep.scalalib.ScalaCoreCCodeGen
import deep._

class MyCompiler(val DSL: ListDSLOps, name: String, offlineOptim: Boolean = false, lowering: Int = 0) extends Compiler[ListDSLOps] {
  
  // Pipeline Definition:
  
  pipeline += DCE
  
  if (offlineOptim) {
    
    pipeline += PartiallyEvaluate 
    
    pipeline += new Optim.HighLevel(DSL)
    pipeline += new Optim.Generic(DSL)
    
    pipeline += DCE
    
  }
  
  if (lowering > 0) {
    
    pipeline += new ListLowering(DSL)
    
    pipeline += DCE
    
    if (lowering > 1) {
      
      pipeline += new ArrBufLowering(DSL)
      
      pipeline += DCE
      
    }
    
    if (offlineOptim) {
      
      pipeline += PartiallyEvaluate
      
      pipeline += new Optim.Generic(DSL)
      
      pipeline += DCE
      
    }
    
  }
  
  
  // Outputting Scala and C code inside an executable wrapper:
  
  import pardis.prettyprinter._
  
  val codeGenerator = 
      new ScalaCodeGenerator with ASTCodeGenerator[ListDSLOps] {
        val IR = DSL
        import pardis.utils.document.Document
        override def header(): Document = s"""
          |package list
          |import list.shallow._
          |import scala.collection.mutable.ArrayBuffer""".stripMargin
        override def getTraitSignature(): Document = s"""
          |object $name {
          |  def main(args: Array[String]): Unit = """.stripMargin
        override def footer(): Document = s"""
          |}
          |""".stripMargin
      }
  
}
