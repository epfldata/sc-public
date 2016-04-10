package relation
package shallow

import ch.epfl.data.sc.pardis.annotations._

@deep
@reflect[List[_]]
class MirrorList[A]

object MirrorList {
  def apply[A](elems: A*): MirrorList[A] = ???
}

class ArrayExtra

object ArrayExtra {
  def __for(start: Int, end: Int)(f: Int => Unit): Unit = for(i <- 0 until end) f(i)
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
  def join(o: Relation, leftKey: String, rightKey: String): Relation = {
    val newSchema = new Schema(schema.columns ++ o.schema.columns.filter(_ != rightKey))
    val joinedRows = for(r1 <- underlying; 
        r2 <- o.underlying if r1.getField(schema, leftKey) == r2.getField(o.schema, rightKey)) yield
      new Row(r1.values ++ r2.values.zip(o.schema.columns).filter(_._2 != rightKey).map(_._1))
    new Relation(newSchema, joinedRows)
  }
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
