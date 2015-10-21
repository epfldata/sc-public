package mylib
package compiler

import shallow._
import deep._

object Main extends App {
  
  implicit object Context extends MyLibDSL // with Optim.Online // FIXME: does not work when put here... why?
  
  def pgrm = dsl"""
      
    val zero = List[Int]().size
      
    val ls = List(1, 2, 3)
    
    val r = ls map (_ + 1) map (_.toDouble)
    
    (r, zero)
    
  """
  
  {
    import Context._  // needed to provide the `compile` methods with an implicit TypeRep
    
    new MyCompiler(Context).compile(pgrm, "src/main/scala/GeneratedApp")
  }
  
}


