package relation

object Examples extends App {
  
  import RelationDSL.Predef._
  
  def pgrmA = ir"""
    val schema = Schema("number", "digit")
    val En = Relation.scan("data/En.csv", schema, "|")
    val selEn = En.select(x => x.getField(schema, "number") == "one")
    val projEn = selEn.project(Schema("digit", "number"))
    projEn.print
  """
  
  println(pgrmA.transformWith(RelationLowering).transformWith(SchemaSpecialization))
}
