package mylib
package compiler

import shallow._
import deep._

object Main extends App {
   //implicit val _ = ch.epfl.data.sc.pardis.quasi.MacroUtils.ApplyDebug
  
  implicit object Context extends MyLibDSLOps
  
  def pgrm = ???
  
  {
    import Context._  // needed to provide the `compile` methods with an implicit TypeRep
    
    // Creates the directories if they do not already exist
    new java.io.File("generator-out/src/main/scala").mkdirs()
    
    new MyCompiler(Context, "GenApp", offlineOptim = false).compile(pgrm, "src/main/scala/GenApp")
  }
  
}


/*
  dsl"""
      
    val zero = List[Int]().size
    
    zero
    
  """
*/

