package relation
package shallow

import java.util.NoSuchElementException

import ch.epfl.data.sc.pardis.annotations._

@deep
@reflect[List[_]]
class MirrorList[A]

object MirrorList {
  def apply[A](elems: A*): MirrorList[A] = ???
}

/** A row contained in a relation */
@deep
@needs[List[_] :: Schema]
class Row(val values: List[String]) {
  /** Access a field of this row, given the relation schema and the field name */
  def getField(schema: Schema, fieldName: String): String = schema.columns.zip(values).collectFirst({ 
    case (field, value) if field == fieldName =>
      value
  }).get

  override def toString: String = values.mkString("|")
}

/** Schema of a relation, storing the names of each columns (fields) */
@deep
@needs[List[_]]
class Schema(val columns: List[String]) {
  def size = columns.size
  def indexOf(columnName: String) = {
    val i = columns.indexOf(columnName)
    if (i < 0) throw new NoSuchElementException
    i
  }
}
object Schema {
  @pure
  def apply(columns: String*): Schema = new Schema(columns.toList)
}

/** The class representing a relation (database table or intermediate result) */
@deep
@needs[Row :: Schema :: List[_]]
class Relation(val schema: Schema, val underlying: List[Row]) {
  /** Selection operation: filters the rows, removing those that do not satisfy predicate `p` */
  def select(p: Row => Boolean): Relation = {
    new Relation(schema, underlying.filter(p))
  }
  /** Projection operation: reduces the columns of a relation according to a new schema */
  def project(newSchema: Schema): Relation = {
    new Relation(newSchema, underlying.map(r => {
      val (_, v) = schema.columns.zip(r.values).filter(sv => newSchema.columns.contains(sv._1)).unzip
      new Row(v)
      })
    )
  }
  /** Equi-join operation: combines each rows from two tables when their keys match
    * the result relation does not have the join-key column from the right relation */
  def join(o: Relation, leftKey: String, rightKey: String): Relation = {
    val newSchema = new Schema(schema.columns ++ o.schema.columns.filter(_ != rightKey))
    val joinedRows = for(r1 <- underlying; 
        r2 <- o.underlying if r1.getField(schema, leftKey) == r2.getField(o.schema, rightKey)) yield
      new Row(r1.values ++ r2.values.zip(o.schema.columns).filter(_._2 != rightKey).map(_._1))
    new Relation(newSchema, joinedRows)
  }
  
  //def aggregate(key: Schema, agg: (Double, Row) => Double): Relation = ???
  
  /** Prints the relation to standard output */
  def print: Unit = for(r <- underlying) {
    println(r)
  }
  override def toString: String = {
    val sb = new StringBuilder
    sb ++= schema.columns.mkString("|") + "\n"
    for(r <- underlying) {
      sb ++= r.toString
      sb ++= "\n"
    }
    sb.toString
  }
}
object Relation {
  /** Scans a relation from a file, given its schema and a field delimiter */
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
