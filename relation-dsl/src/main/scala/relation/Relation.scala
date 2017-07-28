package relation

import squid.lib.transparent
import squid.quasi.{dbg_embed, embed, phase}

/** A row contained in a relation */
@embed
class Row(val values: List[String]) {
  /** Access a field of this row, given the relation schema and the field name */
  def getField(schema: Schema, fieldName: String): String =
    schema.columns.zip(values).find(x => x._1 == fieldName).map(_._2).get

  override def toString: String = values.mkString("|")
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
      val values = indices.map(idx => r.values(idx))
      new Row(values)
      })
    )
  }
  /** Equi-join operation: combines each rows from two tables when their keys match
    * the result relation does not have the join-key column from the right relation */
  @phase('RelRemove)
  def join(o: Relation, leftKey: String, rightKey: String): Relation = {
    val newSchema = new Schema(schema.columns ++ o.schema.columns.filter(_ != rightKey))
    val joinedRows = for(r1 <- underlying; 
        r2 <- o.underlying if r1.getField(schema, leftKey) == r2.getField(o.schema, rightKey)) yield
      new Row(r1.values ++ r2.values.zip(o.schema.columns).filter(_._2 != rightKey).map(_._1))
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
      buf = new Row(line.split(delimiter.charAt(0)).toList) :: buf
    }
    new Relation(schema, buf)
  }
}
