package mylib
package compiler

import ch.epfl.data._
import sc._
import pardis.types._
import sc.pardis.ir._
import sc.pardis.optimization._
import sc.pardis.compiler._
import deep._

class MyCompiler(val DSL: MyLibDSL, name: String, offlineOptim: Boolean = false, lowering: Int = 0) extends Compiler[MyLibDSL] {
  
  // Pipeline Definition:
  
  pipeline += DCE
  
  if (offlineOptim) {
    
    pipeline += PartiallyEvaluate  // used to crash... for some reason... <- needs DCE to run before!
    
    //pipeline += new Optim.Offline.HighLevel(DSL)
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
      
      //pipeline += new Optim.Offline.Generic(DSL)
      pipeline += new Optim.Generic(DSL)
      
      pipeline += DCE
      
    }
    
  }
  
  
  // Outputting Scala code inside an executable wrapper:
  
  import sc.pardis.prettyprinter._
  
  val codeGenerator = new ScalaCodeGenerator with ASTCodeGenerator[MyLibDSL] {
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

















