package list
package compiler

import ch.epfl.data.sc.pardis
import pardis.optimization._
import pardis.compiler._

import deep._

class MyCompiler(val DSL: ListDSLOps, name: String, offlineOptim: Boolean = false, lowering: Int = 0, cCodeGen: Boolean = false) extends Compiler[ListDSLOps] {
  
  // Pipeline Definition:
  
  pipeline += DCE
  
  if (offlineOptim) {
    
    pipeline += PartiallyEvaluate 
    
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
      
      if (lowering > 2) {

        pipeline += new ArrayBufferToArray(DSL)
        
        pipeline += new CGenLowering(DSL)
        
        pipeline += DCE
        
      }
    }
    
    if (offlineOptim) {
      
      pipeline += PartiallyEvaluate
      
      //pipeline += new Optim.Offline.Generic(DSL)
      pipeline += new Optim.Generic(DSL)
      
      pipeline += DCE
      
    }
    
  }

  if(cCodeGen) {
    pipeline += new CoreLanguageToC(DSL)
  }
  
  
  // Outputting Scala code inside an executable wrapper:
  
  import pardis.prettyprinter._
  
  val codeGenerator = 
    if(!cCodeGen) {
      new ScalaCodeGenerator with ASTCodeGenerator[ListDSLOps] {
        val IR = DSL
        import pardis.utils.document.Document
        override def getHeader(): Document = s"""
          |package list
          |import list.shallow._
          |import scala.collection.mutable.ArrayBuffer""".stripMargin
        override def getTraitSignature(): Document = s"""
          |object $name {
          |  def main(args: Array[String]): Unit = println(""".stripMargin
        override def getFooter(): Document = s"""
          |  )
          |}
          |""".stripMargin
      }
    } else {
      new CASTCodeGenerator[ListDSLOps] {
        val IR = DSL
        import pardis.utils.document._
        import pardis.ir._
        import pardis.types._
        import shallow._

        implicit val context = DSL

        override val verbose: Boolean = true

        override def pardisTypeToString[A](t: PardisType[A]): String = 
          if(t.isArray) 
            pardisTypeToString(t.typeArguments(0)) + "*"
          else
            super.pardisTypeToString(t)

        override def body(program: PardisProgram): Document = {
          super.body(program) :/: {
            val res = program.main.res
            val ident = (res.tp: Any) match {
              case IntType => "%d"
              case tp => throw new Exception(s"Does not know how to print the type $tp!")
            }
            doc"""printf("$ident", $res);"""
          }
        }

        override def functionNodeToDocument(fun: FunctionNode[_]) = fun match {
          case dsl"Mem.alloc($size)" => {
            val tp = fun.tp.typeArguments(0)
            doc"($tp *)malloc($size * sizeof($tp))"
          }
          case dsl"Mem.free($mem)" => doc"free($mem)"
          case _ if fun.name.startsWith("unary_") /*&& fun.argss.size == 1 && fun.argss.head.size == 1*/ =>
            val name = fun.name.substring("unary_".length)
            val arg = fun.caller.get
            doc"$name($arg)"
          case _ => super.functionNodeToDocument(fun)
        }
      }
    }
  
}
