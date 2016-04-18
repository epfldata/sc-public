package relation
package compiler

import shallow._
import deep._

object Main extends App {
  
  implicit object Context extends RelationDSLOpsPackaged
  
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
    
    new RelationCompiler(Context, "GenApp").compile(pgrm, "src/main/scala/GenApp")
  }  
}
