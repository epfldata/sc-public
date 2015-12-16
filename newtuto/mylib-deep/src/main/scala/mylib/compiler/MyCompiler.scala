package mylib
package compiler

import ch.epfl.data._
import sc._
import pardis.types._
import sc.pardis.ir._
import sc.pardis.optimization._
import sc.pardis.compiler._
import deep._

class MyCompiler(val DSL: MyLibDSLOps, name: String, offlineOptim: Boolean = false, lowering: Int = 0) extends Compiler[MyLibDSLOps] {
  
  // Pipeline Definition:
  
  pipeline += DCE
  
  if (offlineOptim) {
    
    pipeline += PartiallyEvaluate
    
    pipeline += new Optim(DSL)
    
    pipeline += DCE
    
  }
  
  if (lowering > 0) {
    
    pipeline += new Lowering(DSL)
    
    pipeline += DCE
    
  }
  
  
  // Outputting Scala code inside an executable wrapper:
  
  import sc.pardis.prettyprinter._
  
  val codeGenerator = new ScalaCodeGenerator /*with ASTCodeGenerator[MyLibDSLOps]*/ {
    val IR = DSL
    import sc.pardis.utils.document.Document
    override def getHeader(): Document = s"""
      |package mylib
      |import mylib.shallow._
      |import scala.collection.mutable.ArrayBuffer""".stripMargin
    override def getTraitSignature(): Document = s"""
      |object $name {
      |  def main(args: Array[String]): Unit = println(""".stripMargin
    override def getFooter(): Document = s"""
      |  )
      |}
      |""".stripMargin
  }
  
}

















