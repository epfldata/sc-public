package relation
package compiler

import scala.collection.mutable.ArrayBuffer

import ch.epfl.data.sc.pardis
import pardis.optimization.RecursiveRuleBasedTransformer
import pardis.quasi.TypeParameters._
import pardis.types._
import PardisTypeImplicits._
import pardis.ir._

import relation.deep.RelationDSLOpsPackaged
import relation.shallow._  
import ArrayExtra.__for

class RelationRecordLowering(override val IR: RelationDSLOpsPackaged, override val schemaAnalysis: SchemaAnalysis) extends RelationLowering(IR, schemaAnalysis) {
  import IR.Predef._
  import IR.{__new, field, __lambda}

  private var recordsCount = 0

  def getClassTag = {
    recordsCount += 1
    StructTags.ClassTag(s"Record$recordsCount")
  }

  class Rec

  type LoweredRelation = Rep[Array[Rec]]

  def getLoweredArray(relation: Rep[Relation]): Rep[Array[Rec]] = getRelationLowered(relation)

  def relationScan(scanner: Rep[RelationScanner], schema: Schema, resultRelation: Rep[Relation]): LoweredRelation = {
    implicit val recTp: TypeRep[Rec] = new RecordType[Rec](getClassTag, None)
      def loadRecord: Rep[Rec] = __new[Rec](schema.columns.map(column => (column, false, dsl"$scanner.next_string()")): _*)

      dsl"""
        val arr = new Array[Rec](3)
        var i = 0
        while($scanner.hasNext) {
          val rec = $loadRecord
          arr(i) = rec
          i = i + 1
        }
        arr
      """
  }
  def relationProject(relation: Rep[Relation], schema: Schema, resultRelation: Rep[Relation]): LoweredRelation = {
    val arr = getLoweredArray(relation)
    implicit val recTp: TypeRep[Rec] = new RecordType[Rec](getClassTag, None)
      def copyRecord(e: Rep[Any]): Rep[Rec] = __new[Rec](schema.columns.map(column => (column, false, field[String](e, column))): _*)
      val newArr = dsl"new Array[Rec]($arr.length)"
      dsl"""
        Range(0, $arr.length) foreach ${ __lambda[Int,Unit]((x: Rep[Int]) =>
          dsl"$newArr($x) = ${ copyRecord( dsl"$arr($x)" ) }"
        )}
      """
      newArr
  }
  def relationSelect(relation: Rep[Relation], field: String, value: Rep[String], resultRelation: Rep[Relation]): LoweredRelation = {
    val arr = getLoweredArray(relation)
    implicit val recTp: TypeRep[Rec] = arr.tp.typeArguments(0).asInstanceOf[TypeRep[Rec]]
      dsl"""
        var size = 0
        for(j <- Range(0, $arr.length)) {
          val e = $arr(j)
          if(__struct_field[String](e, $field) == $value) {
            size = size + 1
          }
        }
        val arr = new Array[Rec](size)
        var i = 0
        for(j <- Range(0, arr.length)) {
          val e = $arr(j)
          if(__struct_field[String](e, $field) == $value) {
            arr(i) = $arr(j)
            i = i + 1
          }
        }
        arr
      """
  }
  def relationJoin(leftRelation: Rep[Relation], rightRelation: Rep[Relation], leftKey: String, rightKey: String, resultRelation: Rep[Relation]): LoweredRelation = {
    implicit val recTp: TypeRep[Rec] = new RecordType[Rec](getClassTag, None)
    val arr1 = getLoweredArray(leftRelation)
    val arr2 = getLoweredArray(rightRelation)
      val sch1 = getRelationSchema(leftRelation)
      val sch2 = getRelationSchema(rightRelation)
      val sch1List = sch1.columns
      val sch2List = sch2.columns.filter(_ != rightKey)
      val newSchema = getRelationSchema(resultRelation)
      def joinRecords(e1: Rep[Any], e2: Rep[Any]): Rep[Rec] = {
        __new[Rec](sch1List.map(column => (column, false, field[String](e1, column))) ++ 
          sch2List.map(column => (column, false, field[String](e2, column))): _*)
      }
      def iterateOverTwoLists[T](f: (Rep[Any], Rep[Any]) => Rep[Unit]): Rep[Unit] = {
        dsl"""Range(0, $arr1.length) foreach ${
          __lambda[Int, Unit]({ (i: Rep[Int]) => 
          val e1 = dsl"$arr1($i)"
          dsl"""Range(0, $arr2.length) foreach ${
            __lambda[Int, Unit]({ (j: Rep[Int]) => 
              val e2 = dsl"$arr2($j)"
              f(e1, e2)
            })
          }"""
          })
        }"""
      }
      val size = newVar(dsl"0")
      iterateOverTwoLists((x, y) => 
        dsl"if(__struct_field[String]($x, $leftKey) == __struct_field[String]($y, $rightKey)) $size = $size + 1"
      )
      val arr = dsl"new Array[Rec]($size)"
      val index = newVar(dsl"0")
      iterateOverTwoLists((x, y) => 
        dsl"""
        if(__struct_field[String]($x, $leftKey) == __struct_field[String]($y, $rightKey)) {
          $arr($index) = ${joinRecords(x, y)}
          $index = $index + 1
        }"""
      )
      arr
  }
  def relationPrint(relation: Rep[Relation]): Unit = {
    val arr = getLoweredArray(relation)
    implicit val recTp: TypeRep[Rec] = arr.tp.typeArguments(0).asInstanceOf[TypeRep[Rec]]
    dsl"""
        for(j <- Range(0, $arr.length)) {
          val e = $arr(j)
          println(e)
        }
      """
      ()
  }

}
