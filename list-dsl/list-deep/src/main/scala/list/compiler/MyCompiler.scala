package list
package compiler

import ch.epfl.data.sc.pardis
import pardis.optimization._
import pardis.compiler._
import pardis.deep.scalalib.ScalaCoreCCodeGen
import deep._

class MyCompiler(val DSL: ListDSLOps, name: String, offlineOptim: Boolean = false, lowering: Int = 0, cCodeGen: Boolean = false) extends Compiler[ListDSLOps] {
  
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
      
      if (lowering > 2) {

        pipeline += new ArrayBufferToArray(DSL)
        
        pipeline += new CGenLowering(DSL)
        
        pipeline += DCE
        
      }
    }
    
    if (offlineOptim) {
      
      pipeline += PartiallyEvaluate
      
      pipeline += new Optim.Generic(DSL)
      
      pipeline += DCE
      
    }
    
  }

  if(cCodeGen) {
    require(lowering > 2)
    pipeline += ScalaCoreToC
  }
  
  
  // Outputting Scala and C code inside an executable wrapper:
  
  import pardis.prettyprinter._
  
  val codeGenerator = 
    if(!cCodeGen) {
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
    } else {
      new ScalaCoreCCodeGen with CASTCodeGenerator[ListDSLOps] {
        val IR = DSL
        import IR.Predef._
        import pardis.utils.document._
        import pardis.ir._
        import pardis.types._
        import shallow._
        import pardis.quasi.TypeParameters._

        val params = newTypeParams('A); import params._

        implicit val context = DSL

        override def pardisTypeToString[A](t: PardisType[A]): String = 
          if (t.isArray)
            pardisTypeToString(t.typeArguments(0)) + "*"
          else
            super.pardisTypeToString(t)

        override def functionNodeToDocument(fun: FunctionNode[_]) = fun match {
          case dsl"Mem.alloc[A]($size)" => {
            val tp = implicitly[TypeRep[A]]
            doc"($tp *)malloc($size * sizeof($tp))"
          }
          case dsl"Mem.free($mem)" => doc"free($mem)"
          case _ => super.functionNodeToDocument(fun)
        }
      }
    }
  
}
