package relation
package compiler

import ch.epfl.data.sc.pardis
import ch.epfl.data.sc.pardis.ir._
import ch.epfl.data.sc.pardis.types._
import relation.deep.RelationDSLOpsPackaged
import relation.shallow._

/** A version of the Column Store transformer that partially evaluates the outer array */
class ColumnStoreLoweringPE(override val IR: RelationDSLOpsPackaged, override val schemaAnalysis: SchemaAnalysis) extends RelationLowering(IR, schemaAnalysis) {
  import IR.Predef._
  
  type Value = String
  type Column = Array[String]
  type LoweredRelation = (IndexedSeq[Rep[Column]], Rep[Int])
  
  def relationScan(scanner: Rep[RelationScanner], schema: Schema, size: Rep[Int], resultSchema: Schema): LoweredRelation = {
    
    val arrs = for (c <- 0 until resultSchema.size) yield dsl"new Array[Value]($size)"
    
    dsl"""
      for (i <- 0 until $size) ${ i: Rep[Int] =>
        for (arr <- arrs) dsl"$arr($i) = $scanner.next_string()"
        dsl"()"
      }(i)
    """
    
    arrs.toIndexedSeq -> size
  }
  
  def relationProject(rel: Rep[Relation], schema: Schema, resultSchema: Schema): LoweredRelation = {
    val (arrs,size) = getRelationLowered(rel)
    val res = for (c <- resultSchema.columns) yield arrs(schema.columns.indexOf(c))
    res.toIndexedSeq -> size
  }
  
  def relationSelect(rel: Rep[Relation], field: String, value: Rep[String], resultSchema: Schema): LoweredRelation = {
    val (arrs,size) = getRelationLowered(rel)
    val fieldArr = arrs(resultSchema indexOf field)
    
    val newSize = newVar(dsl"0")
    
    dsl" for (i <- 0 until $size) if ($fieldArr(i) == $value) $newSize = $newSize + 1 "
    val res = for (_ <- arrs.indices) yield dsl"new Array[Value]($newSize)"
    
    dsl"""
      $newSize = 0
      for (i <- 0 until $size)
        if ($fieldArr(i) == $value) {
          ${ i: Rep[Int] => for ((ar,re) <- arrs zip res) dsl"$re($newSize) = $ar($i)"; dsl"()" }(i)
          $newSize = $newSize + 1
        }
    """
    
    res -> dsl"$newSize"
  }
  
  def relationJoin(leftRelation: Rep[Relation], rightRelation: Rep[Relation], leftKey: String, rightKey: String, resultSchema: Schema): LoweredRelation = {
    val (arrs1,size1) = getRelationLowered(leftRelation)
    val (arrs2,size2) = getRelationLowered(rightRelation)
    val sch1 = getRelationSchema(leftRelation)
    val sch2 = getRelationSchema(rightRelation)
    val leftKeyArr = arrs1(sch1.columns.indexOf(leftKey))
    val removedIndex = sch2.columns.indexOf(rightKey)
    val rightKeyArr = arrs2(removedIndex)
    
    val newSize = newVar(dsl"0")
    
    dsl"""for { i <- 0 until $size1; j <- 0 until $size2}
            if ($leftKeyArr(i) == $rightKeyArr(j)) $newSize = $newSize + 1 """
    
    val res = for (_ <- 0 until resultSchema.size) yield dsl"new Array[String]($newSize)"
    
    dsl"""
      $newSize = 0
      for { i <- 0 until $size1; j <- 0 until $size2 }
      if ($leftKeyArr(i) == $rightKeyArr(j)) {  
        ${ (i: Rep[Int], j: Rep[Int]) => 
          for ((ar,re) <- arrs1 zip res)                                              dsl"$re($newSize) = $ar($i)"
          for ((ar,re) <- arrs2.patch(removedIndex, Nil, 1) zip res.drop(arrs1.size)) dsl"$re($newSize) = $ar($j)"
          dsl"()"
        }(i,j)
        $newSize = $newSize + 1
      }
    """
    
    res -> dsl"$newSize"
  }
  
  def relationPrint(rel: Rep[Relation]): Unit = {
    val (arrs,size) = getRelationLowered(rel)
    val schema = getRelationSchema(rel)
    
    dsl"""
      for { i <- 0 until $size } {
        val str = ${ index: Rep[Int] =>
          arrs.iterator.zipWithIndex map {
            case (ar, 0) => dsl"$ar($index)"
            case (ar, _) => dsl""" "|" + $ar($index) """
          } reduceOption ((a,b) => dsl"$a + $b") getOrElse dsl"${""}"
        }(i)
        println(str)
      }
    """
    
  }

}


