package mylib
package compiler

import ch.epfl.data._
import sc._
import pardis.types._
import sc.pardis.ir._
import sc.pardis.optimization._
import sc.pardis.compiler._
import deep._

class MyCompiler(val DSL: MyLibDSL) extends Compiler[MyLibDSL] {
  
  // Pipeline Definition:
  
  //pipeline += DCE
  
  //pipeline += PartiallyEvaluate  // crashes: key not found x1
  
  pipeline += new TransformerHandler {
    override def apply[Lang <: Base, T: PardisType](context: Lang)(block: context.Block[T]): context.Block[T] =
      new Optim.Offline(context.asInstanceOf[MyLibDSL]).optimize(block)
  }
  
  pipeline += DCE
  
  
  // Outputting Scala code inside an executable wrapper:
  
  import sc.pardis.prettyprinter._
  
  val codeGenerator = new ScalaCodeGenerator with ASTCodeGenerator[MyLibDSL] {
    val IR = DSL
    import sc.pardis.utils.document.Document
    override def getHeader(): Document = s"""
      |package mylib
      |import mylib.shallow._""".stripMargin
    override def getTraitSignature(): Document = s"""
      |object GeneratedVectorApp {
      |  def main(args: Array[String]): Unit = println(""".stripMargin
    override def getFooter(): Document = s"""
      |  )
      |}
      |""".stripMargin
  }
  
}

















