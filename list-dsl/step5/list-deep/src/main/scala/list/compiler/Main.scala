package list
package compiler

import shallow._
import deep._

object Main extends App {
  
  implicit object Context extends ListDSLOpsPackaged
  
  def pgrmA = dsl"""
      
    val zero = List[Int]().size
      
    val ls = List(1, 2, 3)
    
    val r = ls map (_ + 1) map (_.toDouble)
    
    r.print
    printf("%d\n", zero)
    printf("%d\n", ls.size)
    
  """
  
  def pgrmB = dsl"""
    val ls = List(1, -2, 3, 0, -42)
    val ls2 = ls map (_ + 1) filter (_ > 0)
    if (ls2.size == 0) {
      List(1,2).size
    } else 0
    printf("%d\n", ls2.size)
    ls2.print
    val ls3 = ((ls2 + 10) + 12)
    ls3.print
  """
  
  def pgrmC = dsl"""
    val ls = List(1, -2, 3, 0, -42)
    
    val indices = ls.fold [(List[Int], Int) ] ( (List[Int](), 0), { (ls_i,_) =>
      val ls = ls_i._1
      val i = ls_i._2
      (ls + i, i + 1)
    })
    ls.print
    val ls2 = List.zip(ls, indices._1)
    printf("%d\n", ls2.size)
  """
  
  def pgrm = pgrmA
  
  {
    import Context.Predef._  // needed to provide the `compile` methods with an implicit TypeRep
    
    // Creates the directories if do not already exist!
    new java.io.File("generator-out/src/main/scala").mkdirs()
    
    new MyCompiler(Context, "GenApp", offlineOptim = false).compile(pgrm, "src/main/scala/GenApp")
    new MyCompiler(Context, "GenAppOpt", offlineOptim = true).compile(pgrm, "src/main/scala/GenAppOpt")
    new MyCompiler(Context, "GenAppLow", lowering = 1).compile(pgrm, "src/main/scala/GenAppLow")
    new MyCompiler(Context, "GenAppOptLow", offlineOptim = true, lowering = 1).compile(pgrm, "src/main/scala/GenAppOptLow")
    new MyCompiler(Context, "GenAppOptLowLow", offlineOptim = true, lowering = 2).compile(pgrm, "src/main/scala/GenAppOptLowLow")
    new MyCompiler(Context, "GenAppOptLowLowC", offlineOptim = true, lowering = 3).compile(pgrm, "src/main/scala/GenAppOptLowLowC")
    new MyCompiler(Context, "Main", offlineOptim = true, lowering = 3, cCodeGen = true).compile(pgrm, "Main")
  }
  
}

