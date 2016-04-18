

sealed trait Lang
case object Eng extends Lang
case object Fra extends Lang

object Main {
  def main(args: Array[String]) {
  	val lang: Lang = Eng
  	val oneToNine = List("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
  	val tenToNineteen = List("ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
 	val twentyToNinety = List("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

 	val oneToNinetyNine = oneToNine ++ tenToNineteen ++ twentyToNinety.flatMap(l => l :: oneToNine.map(n => s"$l-$n"))
 	val oneTo999 = oneToNinetyNine ++ oneToNine.flatMap(n => s"$n hundred" :: oneToNinetyNine.map(n2 => s"$n hundred and $n2"))
 	val oneTo9999 = oneTo999 ++ oneToNine.flatMap(n => s"$n thousand" :: oneTo999.map(n2 => s"$n thousand and $n2"))
 	def getRow(x: (Int, String)) = lang match {
 	  case Eng => s"${x._2}|${x._1}"
 	  case Fra => s"${x._1}|${x._2}eaux"
 	}
 	println((1 to 9999).zip(oneTo9999).map(getRow).mkString("\n"))
  }
}