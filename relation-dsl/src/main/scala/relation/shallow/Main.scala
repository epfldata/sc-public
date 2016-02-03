package relation.shallow

object Main extends App {
  val s = Schema("number", "digit")
  val r = Relation.scan("data/R.csv", s, "|")
  r.print
}

