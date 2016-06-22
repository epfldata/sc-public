package relation
package compiler

import ch.epfl.data.sc.pardis
import ch.epfl.data.sc.pardis.ir._
import ch.epfl.data.sc.pardis.types._
import relation.deep.RelationDSLOpsPackaged
import relation.shallow._

/** A version of the Column Store transformer that unrolls all loops with statically-known bounds */
class ColumnStoreLoweringUnroll(override val IR: RelationDSLOpsPackaged, override val schemaAnalysis: SchemaAnalysis) extends RelationLowering(IR, schemaAnalysis) {
  import IR.Predef._
  
  type Column = Array[String]
  type LoweredRelation = (Rep[Array[Column]], Rep[Int])
  
  def relationScan(scanner: Rep[RelationScanner], schema: Schema, size: Rep[Int], resultSchema: Schema): LoweredRelation = {
    val nColumns = resultSchema.size
    
    val arr = dsl"new Array[Column]($nColumns)"
    for (c <- 0 until nColumns) dsl"$arr($c) = new Array[String]($size)"
    
    dsl"""
      for (i <- 0 until $size) ${ i: Rep[Int] =>
        for (c <- 0 until nColumns) dsl"$arr($c)($i) = $scanner.next_string()"
        dsl"()"
      }(i)
    """
    
    arr -> size
  }
  
  def relationProject(rel: Rep[Relation], schema: Schema, resultSchema: Schema): LoweredRelation = {
    val (arr,size) = getRelationLowered(rel)
    val nColumns = resultSchema.size
    val res: Rep[Array[Column]] = dsl"new Array[Column]($nColumns)"
    
    for (c <- 0 until nColumns)
      dsl"$res($c) = $arr(${schema.indexOf(resultSchema.columns(c))})"
    
    res -> size
  }
  
  def relationSelect(rel: Rep[Relation], field: String, value: Rep[String], resultSchema: Schema): LoweredRelation = {
    val (arr,size) = getRelationLowered(rel)
    val fieldIndex = resultSchema.indexOf(field)
    
    val nColumns = resultSchema.size
    val res: Rep[Array[Column]] = dsl"new Array[Column]($nColumns)"
    
    val newSize = newVar(dsl"0")
    
    dsl" for (i <- 0 until $size) if ($arr($fieldIndex)(i) == $value) $newSize = $newSize + 1 "
    for (c <- 0 until nColumns) dsl"$res($c) = new Array[String]($newSize)"
    
    dsl"""
      $newSize = 0
      for (i <- 0 until $size)
        if ($arr($fieldIndex)(i) == $value) {
          ${ i: Rep[Int] => for (c <- 0 until nColumns) dsl"$res($c)($newSize) = $arr($c)($i)"; dsl"()" }(i)
          $newSize = $newSize + 1
        }
    """
    
    res -> dsl"$newSize"
  }
  
  def relationJoin(leftRelation: Rep[Relation], rightRelation: Rep[Relation], leftKey: String, rightKey: String, resultSchema: Schema): LoweredRelation = {
    val (arr1,size1) = getRelationLowered(leftRelation)
    val (arr2,size2) = getRelationLowered(rightRelation)
    val sch1 = getRelationSchema(leftRelation)
    val sch2 = getRelationSchema(rightRelation)
    val leftKeyIndex = sch1.indexOf(leftKey)
    val rightKeyIndex = sch2.indexOf(rightKey)
    
    val res: Rep[Array[Column]] = dsl"new Array[Column](${resultSchema.size})"
    
    val newSize = newVar(dsl"0")
    
    dsl"""for { i <- 0 until $size1; j <- 0 until $size2}
            if ($arr1($leftKeyIndex)(i) == $arr2($rightKeyIndex)(j)) $newSize = $newSize + 1 """
    for (c <- 0 until resultSchema.size) dsl"$res($c) = new Array[String]($newSize)"
    
    val removedIndex = rightKeyIndex
    
    dsl"""
      $newSize = 0
      for { i <- 0 until $size1; j <- 0 until $size2 }
      if ($arr1($leftKeyIndex)(i) == $arr2($rightKeyIndex)(j)) {  
        ${ (i: Rep[Int], j: Rep[Int]) => 
          for (c <- 0 until sch1.size) dsl"$res($c)($newSize) = $arr1($c)($i)"
          var offset = 0
          for (c <- 0 until sch2.size)
            if (c == removedIndex) offset = 1
            else dsl"$res($c + ${sch1.size - offset})($newSize) = $arr2($c)($j)"
          dsl"()"
        }(i,j)
        $newSize = $newSize + 1
      }
    """
    
    res -> dsl"$newSize"
  }
  
  def relationPrint(rel: Rep[Relation]): Unit = {
    val (arr,size) = getRelationLowered(rel)
    val schema = getRelationSchema(rel)
    
    dsl"""
      for { i <- 0 until $size } {
        val str = ${ index: Rep[Int] =>
          schema.columns.indices map {
            case 0 => dsl"$arr(0)($index)"
            case c => dsl""" "|" + $arr($c)($index) """
          } reduceOption ((a,b) => dsl"$a + $b") getOrElse dsl"${""}"
        }(i)
        println(str)
      }
    """
    
  }

}


