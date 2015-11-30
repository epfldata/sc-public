package mylib
package compiler

import shallow._
import deep._

object Main extends App {
   //implicit val _ = ch.epfl.data.sc.pardis.quasi.MacroUtils.ApplyDebug
  
  implicit object Context extends MyLibDSL // with Optim.Online // FIXME: does not work when put here... see `Notes.md`
  
  def pgrmA = dsl"""
      
    val zero = List[Int]().size
      
    val ls = List(1, 2, 3)
    
    val r = ls map (_ + 1) map (_.toDouble)
    
    (r, zero, ls.size)
    
  """
  
  def pgrmB = dsl"""
    val ls = List(1, -2, 3, 0, -42)
    val ls2 = ls map (_ + 1) filter (_ > 0)
    (ls2, ls2.size)
  """
  
  def pgrmC = dsl"""
    val ls = List(1, -2, 3, 0, -42)
    
    //val (indices, sz) = ls.fold[(List[Int], Int)]((List(), 0), { case((ls, i), _) => (ls + i, i+1) })
    //val indices = ls.fold[(List[Int], Int)]((List(), 0), { case((ls, i), _) => (ls + i, i+1) })
    
    //val indices = ls.fold [(List[Int], Int) ] ( (List(), 0), { (ls_i,_) => (ls_i._1 + ls_i._2, ls_i._2 + 1) } )
    
    val indices = ls.fold [(List[Int], Int) ] ( (List[Int](), 0), { (ls_i,_) =>
      val ls = ls_i._1
      val i = ls_i._2
      (ls + i, i + 1)
    })
    
    List zip (ls, indices._1)
  """
  
  def pgrm = pgrmB
  
  {
    import Context._  // needed to provide the `compile` methods with an implicit TypeRep
    
    new MyCompiler(Context, "GenApp", offlineOptim = false).compile(pgrm, "src/main/scala/GenApp")
    new MyCompiler(Context, "GenAppOpt", offlineOptim = true).compile(pgrm, "src/main/scala/GenAppOpt")
    new MyCompiler(Context, "GenAppLow", lowering = 1).compile(pgrm, "src/main/scala/GenAppLow")
    new MyCompiler(Context, "GenAppOptLow", offlineOptim = true, lowering = 1).compile(pgrm, "src/main/scala/GenAppOptLow")
    new MyCompiler(Context, "GenAppOptLowLow", offlineOptim = true, lowering = 2).compile(pgrm, "src/main/scala/GenAppOptLowLow")
  }
  
}


