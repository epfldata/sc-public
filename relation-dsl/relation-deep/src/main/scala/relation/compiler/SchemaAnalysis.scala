package relation
package compiler

import scala.collection.mutable.ArrayBuffer

import ch.epfl.data.sc.pardis
import pardis.optimization.RuleBasedTransformer
import pardis.quasi.TypeParameters._
import pardis.types._
import PardisTypeImplicits._
import pardis.ir._

import relation.deep.RelationDSLOpsPackaged
import relation.shallow._  

class SchemaAnalysis(override val IR: RelationDSLOpsPackaged) extends RuleBasedTransformer[RelationDSLOpsPackaged](IR) {
  
  implicit val ctx = IR // for quasiquotes
  
  import IR.Predef._

  object StaticSchema {
    def unapply(schema: Rep[Schema]): Option[Schema] = schema match {
      case dsl"Schema($xs*)" =>
        val names = xs map { case Constant(x) => x case _ => return None }
        Some(new Schema(names.toList))
      case _ => None
    }
  }

  val symbolSchema = scala.collection.mutable.Map[Rep[_], Schema]()

  analysis += symRule {
    case rel @ dsl"Relation.scan(${Constant(fileName)}, ${StaticSchema(schema)}, ${Constant(delimiter)})" => 
      symbolSchema += rel -> schema
      ()
  }

  analysis += symRule {
    case rel @ dsl"($rel1: Relation).project(${StaticSchema(schema)})" => 
      symbolSchema += rel -> schema
      ()
  }

  analysis += symRule {
    case rel @ dsl"($rel1: Relation).select((x: Row) => x.getField($_, $name) == ($value: String))" => {
      symbolSchema += rel -> symbolSchema(rel1)
      ()
    }
  }

  analysis += symRule {
    case relr @ dsl"($rel1: Relation).join($rel2, $key1, $key2)" => 
      val sch1 = symbolSchema(rel1)
      val sch2 = symbolSchema(rel2)
      val (Constant(leftKey), Constant(rightKey)) = key1 -> key2
      val sch1List = sch1.columns
      val sch2List = sch2.columns.filter(_ != rightKey)
      val newSchema = new Schema(sch1List ++ sch2List)
      symbolSchema += relr -> newSchema
      ()
  }
}
