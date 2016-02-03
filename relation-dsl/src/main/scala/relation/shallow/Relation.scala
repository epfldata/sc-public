package relation
package shallow

import ch.epfl.data.sc.pardis.annotations._

@deep
@reflect[List[_]]
class MirrorList[A]

object MirrorList {
  def apply[A](elems: A*): MirrorList[A] = ???
}

@deep
@needs[List[_] :: Schema]
class Row(val values: List[String]) {
  def getField(schema: Schema, fieldName: String): String = schema.columns.zip(values).collectFirst({ 
    case (field, value) if field == fieldName =>
      value
  }).get

  override def toString: String = values.mkString(", ")
}

@deep
@needs[List[_]]
class Schema(val columns: List[String])

object Schema {
  def apply(columns: String*): Schema = new Schema(columns.toList)
}

@deep
@needs[Row :: Schema :: List[_]]
class Relation(val schema: Schema, val underlying: List[Row]) {
  def select(p: Row => Boolean): Relation = {
    new Relation(schema, underlying.filter(p))
  }
  def project(newSchema: Schema): Relation = {
    new Relation(newSchema, underlying.map(r => {
      val (_, v) = schema.columns.zip(r.values).filter(sv => newSchema.columns.contains(sv._1)).unzip
      new Row(v)
      })
    )
  }
  def join(o: Relation, cond: (Row, Row) => Boolean): Relation = ???
  def aggregate(key: Schema, agg: (Double, Row) => Double): Relation = ???
  def print: Unit = for(r <- underlying) {
    println(r)
  }
  override def toString: String = {
    val sb = new StringBuilder
    sb ++= schema.columns.mkString(", ") + "\n"
    for(r <- underlying) {
      sb ++= r.toString
      sb ++= "\n"
    }
    sb.toString
  }
}

object Relation {
  // FIXME delimiter should be Char, but because we don't have liftEvidence for Char we don't do it now.
  def scan(filename: String, schema: Schema, delimiter: String): Relation = {
    val sc = new java.util.Scanner(new java.io.File(filename))
    val buf = new scala.collection.mutable.ArrayBuffer[Row]()
    while(sc.hasNext) {
      val line = sc.nextLine
      buf += new Row(line.split(delimiter.charAt(0)).toList)
    }
    new Relation(schema, buf.toList)
  }
}
