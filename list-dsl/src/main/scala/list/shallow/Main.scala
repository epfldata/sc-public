package list.shallow

object Main extends App {
  val ls = List(1,2,3) map (_ + 1) map (_.toDouble)
  println(ls)
}
