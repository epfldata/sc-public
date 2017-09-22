package relation

object Examples extends App {
  
  import RelationDSL.Predef._

  def pgrm0 = ir"""
    val schema = Schema("number", "digit")
    val En = Relation.scan("data/En.csv", schema, "|")
    val selEn = En.select(x => x.getField(schema, "number") == "one")
    selEn.print
  """
  
  def pgrmA = ir"""
    val schema = Schema("number", "digit")
    val En = Relation.scan("data/En.csv", schema, "|")
    val selEn = En.select(x => x.getField(schema, "number") == "one")
    val projEn = selEn.project(Schema("digit", "number"))
    projEn.print
  """

  def pgrmB = ir"""
    val EnSchema = Schema("number", "digit")
    val En = Relation.scan("data/En.csv", EnSchema, "|")
    val FrSchema = Schema("digit", "nombre")
    val Fr = Relation.scan("data/Fr.csv", FrSchema, "|")
    val EnFr = En.join(Fr, "digit", "digit")
    EnFr.print
  """

  def pgrmC = ir"""
    val EnSchema = Schema("number", "digit")
    val En = Relation.scan("data/En.csv", EnSchema, "|")
    val FrSchema = Schema("digitf", "nombre")
    val Fr = Relation.scan("data/Fr.csv", FrSchema, "|")
    val EnFr = En.join(Fr, "digit", "digitf")
    EnFr.project(Schema("digit", "number", "nombre")).print
  """

  def pgrmD = ir"""
    val schema = Schema("number", "digit", "nombre")
    val En = Relation.scan("data/EnFr.csv", schema, "|")
    val selEn = En.select(x => x.getField(schema, "number") == "one")
    val projEn = selEn.project(Schema("digit", "number"))
    projEn.print
  """


  def pgrm = pgrmB
  
  println(pgrm
    transformWith RelationLowering
    transformWith SchemaSpecialization
    transformWith ListFusion
    transformWith RowLayout
    transformWith ListToArrayBuffer
    transformWith HashMapToOpenHashMap
    transformWith OpenHashMapLowering
    transformWith ArrayBufferColumnar
  )
}
