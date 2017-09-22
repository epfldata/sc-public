package relation.shallow

object Main extends App {
  val schema = Schema("number", "digit")
  val en = Relation.scan("data/En.csv", schema, "|")
  en.print
}

object PgrmA extends App {
    val schema = Schema("number", "digit")
    val En = Relation.scan("data/En.csv", schema, "|")
    val selEn = En.select(x => x.getField(schema, "number") == "one")
    val projEn = selEn.project(Schema("number"))
    projEn.print
}

object PgrmB extends App {
    val EnSchema = Schema("number", "digit")
    val En = Relation.scan("data/En.csv", EnSchema, "|")
    val FrSchema = Schema("digit", "nombre")
    val Fr = Relation.scan("data/Fr.csv", FrSchema, "|")
    val EnFr = En.join(Fr, "digit", "digit")
    EnFr.print
}


