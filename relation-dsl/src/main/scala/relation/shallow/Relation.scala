package relation
package shallow

import ch.epfl.data.sc.pardis.annotations._

@deep
@needs[List[_] :: Schema]
class Record(val schema: Schema, val values: List[String]) {
  def getField(name: String): String = schema.columns.zip(values).collectFirst({ case (field, value) if field == name =>
    value
  }).get

  override def toString: String = values.mkString(", ")
}

@deep
@needs[List[_]]
class Schema(val columns: List[String])

@deep
@needs[Record :: Schema :: List[_]]
class Relation(val underlying: List[Record]) {
  def select(p: Record => Boolean): Relation = {
    new Relation(underlying.filter(p))
  }
  def project(schema: Schema): Relation = {
    new Relation(underlying.map(r => {
      val (s, v) = r.schema.columns.zip(r.values).filter(sv => schema.columns.contains(sv._1)).unzip
      new Record(new Schema(s), v)
      })
    )
  }
  def join(o: Relation, cond: (Record, Record) => Boolean): Relation = ???
  def aggregate(key: Schema, agg: (Double, Record) => Double): Relation = ???
  def print: Unit = for(r <- underlying) {
    println(r)
  }
  override def toString: String = {
    val sb = new StringBuilder
    for(r <- underlying) {
      sb ++= r.toString
      sb ++= "\n"
    }
    sb.toString
  }
}

object Relation {
  def scan(filename: String, schema: Schema, delimiter: Char): Relation = {
    val sc = new java.util.Scanner(new java.io.File(filename))
    val buf = new scala.collection.mutable.ArrayBuffer[Record]()
    while(sc.hasNext) {
      val line = sc.nextLine
      buf += new Record(schema, line.split(delimiter).toList)
    }
    new Relation(buf.toList)
  }
}
