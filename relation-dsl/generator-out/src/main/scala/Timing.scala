package relation

object Timing {
  def time[A](a: => A, msg: String) = {
    val start = System.nanoTime
    val result = a
    val end = (System.nanoTime - start) / (1000 * 1000)
    System.out.println(s"$msg completed in ${Console.BLUE}$end${Console.RESET} milliseconds")
    result
  }
}

