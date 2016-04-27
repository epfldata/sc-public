package relation
package shallow

import java.io.FileReader
import java.io.BufferedReader
import java.text.SimpleDateFormat

import ch.epfl.data.sc.pardis.annotations._

// Borrowed from DBLAB

/**
 * A Scanner defined for reading from files.
 *
 * @param filename the input file name
 */
@deep
class RelationScanner(filename: String, delimiter: Char) {
  @dontLift private var byteRead: Int = 0
  @dontLift private var intDigits: Int = 0
  @dontLift private var currentDelimiter: Char = delimiter
  @dontLift private val br: BufferedReader = new BufferedReader(new FileReader(filename))
  @dontLift private val sdf = new SimpleDateFormat("yyyy-MM-dd");

  def next_int() = {
    var number = 0
    var signed = false

    intDigits = 0
    byteRead = br.read()
    if (byteRead == '-') {
      signed = true
      byteRead = br.read()
    }
    while (Character.isDigit(byteRead)) {
      number *= 10
      number += byteRead - '0'
      byteRead = br.read()
      intDigits = intDigits + 1
    }
    if ((byteRead != currentDelimiter) && (byteRead != '.') && (byteRead != '\n'))
      throw new RuntimeException("Tried to read Integer, but found neither currentDelimiter nor . after number (found " +
        byteRead.asInstanceOf[Char] + ", previous token = " + intDigits + "/" + number + ")")
    if (signed) -1 * number else number
  }

  def next_double() = {
    val numeral: Double = next_int()
    var fractal: Double = 0.0
    // Has fractal part
    if (byteRead == '.') {
      fractal = next_int()
      while (intDigits > 0) {
        fractal = fractal * 0.1
        intDigits = intDigits - 1
      }
    }
    if (numeral >= 0) numeral + fractal
    else numeral - fractal
  }

  def next_char() = {
    byteRead = br.read()
    val del = br.read() //currentDelimiter
    if ((del != currentDelimiter) && (del != '\n'))
      throw new RuntimeException("Expected currentDelimiter after char. Not found. Sorry!")
    byteRead.asInstanceOf[Char]
  }

  def next(buf: Array[Byte]): Int = {
    next(buf, 0)
  }

  def next(buf: Array[Byte], offset: Int) = {
    var cnt = offset
    var continue = true
    while (continue && br.ready()) {
      byteRead = br.read()
      if (byteRead != '\r') {
        continue = (byteRead != currentDelimiter) && (byteRead != '\n')
        if (continue) {
          buf(cnt) = byteRead.asInstanceOf[Byte]
          cnt += 1
        }
      }
    }
    cnt
  }

  def next_string(): String = {
    val buf = new Array[Byte](100)
    val cnt = next(buf)
    new String(buf.slice(0, cnt).map(_.toChar))
  }

  def next_date: Int = {
    currentDelimiter = '-'
    val year = next_int
    val month = next_int
    currentDelimiter = delimiter
    val day = next_int
    //val date_str = year + "-" + month + "-" + day
    year * 10000 + month * 100 + day
  }

  def hasNext() = {
    val f = br.ready()
    if (!f) br.close
    f
  }
}

object RelationScanner {
  def getNumLinesInFile(filePath: String): Int = {
    scala.io.Source.fromFile(filePath).getLines.size
  }
}

