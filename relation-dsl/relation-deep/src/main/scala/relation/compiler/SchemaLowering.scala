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

class SchemaLowering(override val IR: RelationDSLOpsPackaged) extends RecursiveRuleBasedTransformer[RelationDSLOpsPackaged](IR) {
  
  implicit val ctx = IR // for quasiquotes
  
  import IR.Predef._
  import IR.{__new, field, __lambda}

  def convertToStaticSchema(schema: Rep[Schema]): Schema = {
  	schema match {
  		case dsl"Schema($xs*)" => 
  		  val names = for(Constant(x) <- xs) yield x
  		  new Schema(names.toList)
  	}
  }

  private var recordsCount = 0

  def getClassTag = {
  	recordsCount += 1
  	StructTags.ClassTag(s"Record$recordsCount")
  }

  class Rec

  rewrite += symRule {
  	case dsl"Relation.scan(${Constant(fileName)}, $schema, ${Constant(delimiter)})" => 
  		val constantSchema = convertToStaticSchema(schema)
  		val scanner = dsl"new RelationScanner($fileName, ${delimiter.charAt(0)})"

  		implicit val recTp: TypeRep[Rec] = new RecordType[Rec](getClassTag, None)
  		def loadRecord: Rep[Rec] = __new[Rec](constantSchema.columns.map(column => (column, false, dsl"$scanner.next_string()")): _*)

  		dsl"""
  		    val arr = new Array[Rec](3)
  		    var i = 0
  			while($scanner.hasNext) {
  				val rec = $loadRecord
  				arr(i) = rec
  				i = i + 1
  			}
  			arr
  		""".asInstanceOf[Rep[Any]]
  }

  rewrite += symRule {
  	case dsl"(${ArrFromRelation(arr)}: Relation).project($schema)" => 
  		val constantSchema = convertToStaticSchema(schema)

  		implicit val recTp: TypeRep[Rec] = new RecordType[Rec](getClassTag, None)
  		def copyRecord(e: Rep[Any]): Rep[Rec] = __new[Rec](constantSchema.columns.map(column => (column, false, field[String](e, column))): _*)
  		val newArr = dsl"new Array[Rec]($arr.length)"
  		import IR.RangeRep
  		IR.Range(dsl"0", dsl"$arr.length").foreach(__lambda({ (j: Rep[Int]) => 
			val e = dsl"$arr($j)"
  			dsl"$newArr($j) = ${copyRecord(e)}"
  		}))
		newArr.asInstanceOf[Rep[Any]]
  }

  rewrite += symRule {
  	case dsl"(${ArrFromRelation(arr)}: Relation).select((x: Row) => x.getField($name) == ($value: String))" => {
  		implicit val recTp: TypeRep[Rec] = arr.tp.typeArguments(0).asInstanceOf[TypeRep[Rec]]
  		dsl"""
  			var size = 0
  			Range(0, $arr.length).foreach({ j => 
  				val e = $arr(j)
  				if(__struct_field[String](e, $name) == $value) {
  					size = size + 1
  				}
  			})
  			val arr = new Array[Rec](size)
  			var i = 0
  			Range(0, arr.length).foreach({ j => 
  				val e = $arr(j)
  				if(__struct_field[String](e, $name) == $value) {
  					arr(i) = $arr(j)
  					i = i + 1
  				}
  			})
  			arr
  		"""
  		// dsl"println($value)"
  	}
  }

  rewrite += symRule {
  	case dsl"(${ArrFromRelation(arr)}: Relation).print" => {
  		implicit val recTp: TypeRep[Rec] = arr.tp.typeArguments(0).asInstanceOf[TypeRep[Rec]]
  		dsl"""
  			Range(0, $arr.length).foreach({ j => 
  				val e = $arr(j)
  				println(e)
  			})
  		"""
  		// dsl"println($value)"
  	}
  }

  object ArrFromRelation {
    def unapply(x: Rep[Relation]): Option[Rep[Array[Rec]]] = x match {
      case dsl"$ls: Relation" =>
        Some(apply[Any](ls).asInstanceOf[Rep[Array[Rec]]])
      case _ => None
    }
  }

}
