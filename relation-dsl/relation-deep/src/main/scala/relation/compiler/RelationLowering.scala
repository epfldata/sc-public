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

abstract class RelationLowering(override val IR: RelationDSLOpsPackaged, val schemaAnalysis: SchemaAnalysis) extends RecursiveRuleBasedTransformer[RelationDSLOpsPackaged](IR) {
  
  implicit val ctx = IR // for quasiquotes
  
  import IR.Predef._

  val loweredRelations = scala.collection.mutable.Map[Rep[Relation], LoweredRelation]()

  def getRelationSchema(relation: Rep[Relation]): Schema = schemaAnalysis.symbolSchema(relation)

  def getRelationLowered(relation: Rep[Relation]): LoweredRelation = loweredRelations(relation)

  type LoweredRelation

  def relationScan(scanner: Rep[RelationScanner], schema: Schema, size: Rep[Int], resultSchema: Schema): LoweredRelation
  def relationProject(relation: Rep[Relation], schema: Schema, resultSchema: Schema): LoweredRelation
  def relationSelect(relation: Rep[Relation], field: String, value: Rep[String], resultSchema: Schema): LoweredRelation
  def relationJoin(leftRelation: Rep[Relation], rightRelation: Rep[Relation], leftKey: String, rightKey: String, resultSchema: Schema): LoweredRelation
  def relationPrint(relation: Rep[Relation]): Unit

  rewrite += symRule {
    case rel @ dsl"Relation.scan(${Constant(fileName)}, $schema1, ${Constant(delimiter)})" => 
      val relation = rel.asInstanceOf[Rep[Relation]]
      val schema = getRelationSchema(relation)
      val scanner = dsl"new RelationScanner($fileName, ${delimiter.charAt(0)})"
      val size = dsl"RelationScanner.getNumLinesInFile($fileName)"

      val res = relationScan(scanner, schema, size, schema)

      loweredRelations += relation -> res

      unit()
  }

  rewrite += symRule {
    case rel @ dsl"($rel1: Relation).project($schema1)" => 
    val relation = rel.asInstanceOf[Rep[Relation]]
      val schema = getRelationSchema(relation)

      val res = relationProject(rel1, schema, schema)

      loweredRelations += relation -> res

      unit()
  }

  rewrite += symRule {
    case rel @ dsl"($rel1: Relation).select((x: Row) => x.getField($_, ${Constant(name)}) == ($value: String))" => {
      val relation = rel.asInstanceOf[Rep[Relation]]
      val res = relationSelect(rel1, name, value, getRelationSchema(relation))

      loweredRelations += relation -> res

      unit()
    }
  }

  rewrite += symRule {
    case rel @ dsl"($rel1: Relation).select($f)" => {
      
      throw new Exception(s"The only supported function for selection is `x => x.getField(schema, name) == value`!")

      unit()
    }
  }

  rewrite += symRule {
    case relr @ dsl"($rel1: Relation).join($rel2, $key1, $key2)" => {
      val relation = relr.asInstanceOf[Rep[Relation]]
      val (Constant(leftKey), Constant(rightKey)) = key1 -> key2

      val res = relationJoin(rel1, rel2, leftKey, rightKey, getRelationSchema(relation))

      loweredRelations += relation -> res

      unit()
    }
  }

  rewrite += symRule {
    case dsl"($rel: Relation).print" => {
      relationPrint(rel)
      
      unit()
    }
  }
}
