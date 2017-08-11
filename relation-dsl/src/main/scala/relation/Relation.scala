package relation

import squid.lib.transparent
import squid.quasi.{dbg_embed, embed, phase}

import scala.collection.mutable

/** A row contained in a relation */
@embed
class Row(private val values: List[String], val size: Int) {
  /** Access a field of this row, given the relation schema and the field name */
  def getField(schema: Schema, fieldName: String): String =
    schema.columns.zip(values).find(x => x._1 == fieldName).map(_._2).get

  @transparent
  def getValue(index: Int): String =
    values(index)

  @transparent
  def append(r2: Row): Row =
    new Row(values ++ r2.values, size + r2.size)

  override def toString: String = values.mkString("|")
}

object Row {
  @transparent
  def apply(values: List[String], size: Int) = {
    new Row(values, size)
  }
}

/** Schema of a relation, storing the names of each columns (fields) */
@embed
class Schema(val columns: List[String]) {
  @transparent
  def size = columns.size
  @transparent
  def indexOf(columnName: String) = {
    val i = columns.indexOf(columnName)
    i
  }
  @transparent
  def indicesOf(columnNames: List[String]): List[Int] = {
    val columnIndexMap = columns.zipWithIndex.toMap
    columnNames.map(columnIndexMap)
  }
  @transparent
  override def toString: String = s"Schema(${columns map ('"'+_+'"') mkString ","})"
}
object Schema {
//  @transparent
  def apply(columns: String*): Schema = new Schema(columns.toList)
}

/** The class representing a relation (database table or intermediate result) */
@embed
class Relation(val schema: Schema, val underlying: List[Row]) {
  /** Selection operation: filters the rows, removing those that do not satisfy predicate `p` */
  @phase('RelRemove)
  def select(p: Row => Boolean): Relation = {
    new Relation(schema, underlying.filter(p))
  }
  /** Projection operation: reduces the columns of a relation according to a new schema */
  @phase('RelRemove)
  def project(newSchema: Schema): Relation = {
    new Relation(newSchema, underlying.map(r => {
      val indices = schema.indicesOf(newSchema.columns)
      val values = indices.map(idx => r.getValue(idx))
      Row(values, newSchema.columns.size)
      })
    )
  }
  /** Equi-join operation: combines each rows from two tables when their keys match. */
  @phase('RelRemove)
  def join(o: Relation, leftKey: String, rightKey: String): Relation = {
    val newSchema = new Schema(schema.columns ++ o.schema.columns)
    val hashTable = new mutable.HashMap[String, Row]
    for(r1 <- underlying) {
      hashTable += ((r1.getField(schema, leftKey), r1))
      ()
    }
    val joinedRows = o.underlying.filter(r2 => hashTable.contains(r2.getField(o.schema, rightKey))).map({ r2 =>
      val r1 = hashTable(r2.getField(o.schema, rightKey))
      r1.append(r2)
    })
    new Relation(newSchema, joinedRows)
  }
  
  //def aggregate(key: Schema, agg: (Double, Row) => Double): Relation = ???
  
  /** Prints the relation to standard output */
  @phase('RelRemove)
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
  @phase('RelRemove)
  def scan(filename: String, schema: Schema, delimiter: String): Relation = {
    val sc = new java.util.Scanner(new java.io.File(filename))
    var buf = List[Row]()
    while(sc.hasNext) {
      val line = sc.nextLine
      buf = Row(line.split(delimiter.charAt(0)).toList, schema.columns.size) :: buf
    }
    new Relation(schema, buf)
  }

//  def scanList(filename: String, schema: Schema, delimiter: String): List[Row] = {
//    val sc = new java.util.Scanner(new java.io.File(filename))
//    var buf = List[Row]()
//    while(sc.hasNext) {
//      val line = sc.nextLineß
//      buf = new Row(line.split(delimiter.charAt(0)).toList) :: buf
//    }
//    buf
//  }
}
