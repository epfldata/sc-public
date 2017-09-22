
object NumberGenerator {
  def main(args: Array[String]) {
    //val lang = 'Eng
    val lang = 'Fra
    
    
    val zero = "zero"
    val oneToNine = List("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
    val tenToNineteen = List("ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
    val twentyToNinety = List("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
    
    val oneToNinetyNine = oneToNine ++ tenToNineteen ++ twentyToNinety.flatMap(l => l :: oneToNine.map(n => s"$l-$n"))
    val oneTo999 = oneToNinetyNine ++ oneToNine.flatMap(n => s"$n hundred" :: oneToNinetyNine.map(n2 => s"$n hundred $n2"))
    val oneTo9999 = oneTo999 ++ oneToNine.flatMap(n => s"$n thousand" :: oneTo999.map(n2 => s"$n thousand $n2"))
    
    
    val zéro = "zéro"
    val unÀNeuf = List("un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf")
    val dixÀDixNeuf = List("dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf")
    val vingtÀQuatreVingtDix = List("vingt", "trente", "quarante", "cinquante", "soixante", Some("soixante"), "quatre-vingt", Some("quatre-vingt"))
    
    val unÀQuatreVingtDixNeuf = unÀNeuf ++ dixÀDixNeuf ++ vingtÀQuatreVingtDix.flatMap {
      case Some(l) => dixÀDixNeuf.map {
        case "onze" if l != "quatre-vingt" => s"$l et onze"
        case n => s"$l-$n"
      }
      case l: String => (l + (if (l=="quatre-vingt") "s" else "")) :: unÀNeuf.map {
        case "un" if l != "quatre-vingt" => s"$l et un"
        case n => s"$l-$n"
      }
    }
    val unÀ999 = unÀQuatreVingtDixNeuf ++ unÀNeuf.flatMap {
      case "un" => s"cent" :: unÀQuatreVingtDixNeuf.map(n2 => s"cent $n2")
      case n => s"$n cents" :: unÀQuatreVingtDixNeuf.map(n2 => s"$n cent $n2")
    }
    val unÀ9999 = unÀ999 ++ unÀNeuf.flatMap {
      case "un" => s"mille" :: unÀ999.map(n2 => s"mille $n2")
      case n => s"$n mille" :: unÀ999.map(n2 => s"$n mille $n2")
    }
    
    
    val (numbers, fmt) = lang match {
      case 'Eng => (zero :: oneTo9999, (x: (Int, String)) => s"${x._2}|${x._1}")
      case 'Fra => (zéro :: unÀ9999, (x: (Int, String)) => s"${x._1}|${x._2}")
    }
    println(0 to 9999 zip numbers map fmt mkString "\n")
  }
}

