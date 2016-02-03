package relation.shallow

object Main extends App {
  val s = Schema("number", "digit")
  val r = Relation.scan("data/R.csv", s, "|")
  r.print
}

object PgrmA extends App {
    val schema = Schema("number", "digit")
    val R = Relation.scan("data/R.csv", schema, "|")
    val selR = R.select(x => x.getField(schema, "number") == "one")
    val projR = selR.project(Schema("number"))
    projR.print
}

object PgrmB extends App {
    val Rschema = Schema("number", "digit")
    val R = Relation.scan("data/R.csv", Rschema, "|")
    val Sschema = Schema("digit", "nombre")
    val S = Relation.scan("data/S.csv", Sschema, "|")
    val RS = R.join(S, "digit", "digit")
    RS.print
}


