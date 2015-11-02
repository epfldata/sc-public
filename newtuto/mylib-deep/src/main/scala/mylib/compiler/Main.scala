package mylib
package compiler

import shallow._
import deep._

object Main extends App {
   //implicit val _ = ch.epfl.data.sc.pardis.quasi.MacroUtils.ApplyDebug
  
  implicit object Context extends MyLibDSL // with Optim.Online // FIXME: does not work when put here... see `Notes.md`
  
  def pgrm = dsl"""
      
    val zero = List[Int]().size
      
    val ls = List(1, 2, 3)
    
    val r = ls map (_ + 1) map (_.toDouble)
    
    (r, zero, List(1).size)
    
  """
  
  {
    import Context._  // needed to provide the `compile` methods with an implicit TypeRep
    
    new MyCompiler(Context, "GeneratedApp", offlineOptim = false).compile(pgrm, "src/main/scala/GeneratedApp")
    new MyCompiler(Context, "GeneratedAppOpt", offlineOptim = true).compile(pgrm, "src/main/scala/GeneratedAppOpt")
  }
  
}


