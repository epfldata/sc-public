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

case class LoweringException(msg: String) extends Exception(msg)

abstract class RelationLowering(override val IR: RelationDSLOpsPackaged, val schemaAnalysis: SchemaAnalysis) extends RecursiveRuleBasedTransformer[RelationDSLOpsPackaged](IR) {
  implicit val ctx = IR // for quasiquotes
  import IR.Predef._
  
  // Methods to provide:
  
  /** The type of a [[relation.shallow.Relation]] after it has been lowered to low-level code */
  type LoweredRelation

  /** Lowering of the [[relation.shallow.Relation.scan]] operation */
  def relationScan(scanner: Rep[RelationScanner], schema: Schema, size: Rep[Int], resultSchema: Schema): LoweredRelation
  
  /** Lowering of the [[relation.shallow.Relation.project]] operation */
  def relationProject(relation: Rep[Relation], schema: Schema, resultSchema: Schema): LoweredRelation
  
  /** Lowering of the [[relation.shallow.Relation.select]] operation */
  def relationSelect(relation: Rep[Relation], field: String, value: Rep[String], resultSchema: Schema): LoweredRelation
  
  /** Lowering of the [[relation.shallow.Relation.join]] operation */
  def relationJoin(leftRelation: Rep[Relation], rightRelation: Rep[Relation], leftKey: String, rightKey: String, resultSchema: Schema): LoweredRelation
  
  /** Lowering of the [[relation.shallow.Relation.print]] operation */
  def relationPrint(relation: Rep[Relation]): Unit


  // Provided methods:
  
  private val loweredRelations = scala.collection.mutable.Map[Rep[Relation], LoweredRelation]()

  def getRelationSchema(relation: Rep[Relation]): Schema = schemaAnalysis.symbolSchema get relation match {
    case Some(s) => s
    case None => throw LoweringException(s"Could not find static schema for relation $relation")
  }

  def getRelationLowered(relation: Rep[Relation]): LoweredRelation = loweredRelations(relation)

  
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
    
    case rel @ dsl"($rel1: Relation).select((x: Row) => x.getField($_, ${Constant(name)}) == ($value: String))" =>
      val relation = rel.asInstanceOf[Rep[Relation]]
      val res = relationSelect(rel1, name, value, getRelationSchema(relation))
  
      loweredRelations += relation -> res
  
      unit()
      
    case rel @ dsl"($rel1: Relation).select($f)" =>
  
      throw LoweringException(s"Selection function should be of the shape `x => x.getField(schema, name) == value`")
  
      unit()
      
  }

  rewrite += symRule {
    case relr @ dsl"($rel1: Relation).join($rel2, $key1, $key2)" =>
      val relation = relr.asInstanceOf[Rep[Relation]]
      val (Constant(leftKey), Constant(rightKey)) = key1 -> key2
  
      val res = relationJoin(rel1, rel2, leftKey, rightKey, getRelationSchema(relation))
  
      loweredRelations += relation -> res
  
      unit()
  }

  rewrite += symRule {
    case dsl"($rel: Relation).print" =>
      relationPrint(rel)
  
      unit()
  }
}
