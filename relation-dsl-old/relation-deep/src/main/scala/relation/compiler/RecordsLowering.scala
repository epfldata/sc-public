package relation
package compiler

import ch.epfl.data.sc.pardis
import pardis.optimization.RecursiveRuleBasedTransformer
import pardis.quasi.TypeParameters._
import pardis.types._
import PardisTypeImplicits._
import pardis.ir._

import relation.deep.RelationDSLOpsPackaged
import relation.shallow._  

class RecordsLowering(override val IR: RelationDSLOpsPackaged, override val schemaAnalysis: SchemaAnalysis) extends RelationLowering(IR, schemaAnalysis) {
  import IR.Predef._
  import IR.{__new}

  private var recordsCount = 0

  def getClassTag = {
    recordsCount += 1
    StructTags.ClassTag(s"Record$recordsCount")
  }

  class Rec

  type LoweredRelation = Rep[Array[Rec]]

  def relationScan(scanner: Rep[RelationScanner], schema: Schema, size: Rep[Int], resultSchema: Schema): LoweredRelation = {
    implicit val recTp: TypeRep[Rec] = new RecordType[Rec](getClassTag, None)
    def loadRecord: Rep[Rec] = __new[Rec](schema.columns.map(column => (column, false, dsl"$scanner.next_string()")): _*)
    dsl"""
      val arr = new Array[Rec]($size)
      var i = 0
      while($scanner.hasNext) {
        val rec = $loadRecord
        arr(i) = rec
        i = i + 1
      }
      arr
    """
  }
  def relationProject(relation: Rep[Relation], schema: Schema, resultSchema: Schema): LoweredRelation = {
    val arr = getRelationLowered(relation)
    implicit val recTp: TypeRep[Rec] = new RecordType[Rec](getClassTag, None)
    val copyRecord: Rep[Any] => Rep[Rec] =
      e => __new[Rec](schema.columns.map(column => (column, false, dsl"__struct_field[String]($e, $column)")): _*)
    val newArr = dsl"new Array[Rec]($arr.length)"
    dsl" for (i <- 0 until $arr.length) $newArr(i) = $copyRecord($arr(i)) "
    newArr
  }
  def relationSelect(relation: Rep[Relation], field: String, value: Rep[String], resultSchema: Schema): LoweredRelation = {
    val arr = getRelationLowered(relation)
    implicit val recTp: TypeRep[Rec] = arr.tp.typeArguments(0).asInstanceOf[TypeRep[Rec]]
    dsl"""
      var size = 0
      for (j <- 0 until $arr.length) {
        val e = $arr(j)
        if(__struct_field[String](e, $field) == $value) {
          size = size + 1
        }
      }
      val arr = new Array[Rec](size)
      var i = 0
      for(j <- 0 until $arr.length) {
        val e = $arr(j)
        if(__struct_field[String](e, $field) == $value) {
          arr(i) = $arr(j)
          i = i + 1
        }
      }
      arr
    """
  }
  def relationJoin(leftRelation: Rep[Relation], rightRelation: Rep[Relation], leftKey: String, rightKey: String, resultSchema: Schema): LoweredRelation = {
    implicit val recTp: TypeRep[Rec] = new RecordType[Rec](getClassTag, None)
    val arr1 = getRelationLowered(leftRelation)
    val arr2 = getRelationLowered(rightRelation)
    val sch1 = getRelationSchema(leftRelation)
    val sch2 = getRelationSchema(rightRelation)
    val sch1List = sch1.columns
    val sch2List = sch2.columns.filter(_ != rightKey)
    def joinRecords(e1: Rep[Any], e2: Rep[Any]): Rep[Rec] = {
      __new[Rec](sch1List.map(column => (column, false, dsl"__struct_field[String]($e1, $column)")) ++ 
        sch2List.map(column => (column, false, dsl"__struct_field[String]($e2, $column)")): _*)
    }
    def iterateOverTwoLists[T](f: (Rep[Any], Rep[Any]) => Rep[Unit]): Rep[Unit] =
      dsl" for (i <- 0 until $arr1.length; j <- 0 until $arr2.length) $f($arr1(i), $arr2(j)) "
    val size = newVar(dsl"0")
    iterateOverTwoLists((x, y) => 
      dsl"if (__struct_field[String]($x, $leftKey) == __struct_field[String]($y, $rightKey)) $size = $size + 1"
    )
    val arr = dsl"new Array[Rec]($size)"
    val index = newVar(dsl"0")
    iterateOverTwoLists((x, y) => 
      dsl"""
        if(__struct_field[String]($x, $leftKey) == __struct_field[String]($y, $rightKey)) {
          $arr($index) = ${joinRecords(x, y)}
          $index = $index + 1
        }
      """
    )
    arr
  }
  def relationPrint(relation: Rep[Relation]): Unit = {
    val arr = getRelationLowered(relation)
    implicit val recTp: TypeRep[Rec] = arr.tp.typeArguments(0).asInstanceOf[TypeRep[Rec]]
    val schema = getRelationSchema(relation)
    val getRecordString = (index: Rep[Int]) => {
      val e = dsl"$arr($index)"
      // Build a string concatenation of the record's fields, separated by "|"
      (schema.columns zip "" #:: (Stream continually "|") foldLeft dsl""" "" """) {
        case (acc, (field, sep)) =>
          val fieldValue = dsl"__struct_field[String]($e, $field)"
          dsl"$acc + $sep + $fieldValue" }
    }
    dsl" for (i <- 0 until $arr.length) println($getRecordString(i)) "
  }

}
