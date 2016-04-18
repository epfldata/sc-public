package relation
package compiler

import shallow._
import deep._
// import ch.epfl.data.sc.pardis.deep.scalalib.ArrayPartialEvaluation

object Main extends App {
  
  implicit object Context extends RelationDSLOpsPackaged //with ArrayPartialEvaluation
  
  def pgrmA = dsl""" 
    val schema = Schema("number", "digit")
    val R = Relation.scan("data/R.csv", schema, "|")
    val selR = R.select(x => x.getField(schema, "number") == "one")
    val projR = selR.project(Schema("number"))
    projR.print
  """

  def pgrmB = dsl"""
    val Rschema = Schema("number", "digit")
    val R = Relation.scan("data/R.csv", Rschema, "|")
    val Sschema = Schema("digit", "nombre")
    val S = Relation.scan("data/S.csv", Sschema, "|")
    val RS = R.join(S, "digit", "digit")
    RS.print
  """
  
  def pgrmC = dsl"""
    val Rschema = Schema("number", "digit")
    val R = Relation.scan("data/R.csv", Rschema, "|").select(x => x.getField(Rschema, "number") == "one")
    val projR = R.project(Schema("number"))
    projR.print
  """
  
  def pgrm = pgrmB
  
  {
    import Context.Predef._  // needed to provide the `compile` methods with an implicit TypeRep
    
    // Creates the directories if do not already exist!
    new java.io.File("generator-out/src/main/scala").mkdirs()
    
    new MyCompiler(Context, "GenApp", offlineOptim = false).compile(pgrm, "src/main/scala/GenApp")
    // new MyCompiler(Context, "GenAppOpt", offlineOptim = true).compile(pgrm, "src/main/scala/GenAppOpt")
    // new MyCompiler(Context, "GenAppLow", lowering = 1).compile(pgrm, "src/main/scala/GenAppLow")
    // new MyCompiler(Context, "GenAppOptLow", offlineOptim = true, lowering = 1).compile(pgrm, "src/main/scala/GenAppOptLow")
    // new MyCompiler(Context, "GenAppOptLowLow", offlineOptim = true, lowering = 2).compile(pgrm, "src/main/scala/GenAppOptLowLow")
    // new MyCompiler(Context, "GenAppOptLowLowC", offlineOptim = true, lowering = 3).compile(pgrm, "src/main/scala/GenAppOptLowLowC")
    // new MyCompiler(Context, "Main", offlineOptim = true, lowering = 3, cCodeGen = true).compile(pgrm, "Main")
  }  
}
