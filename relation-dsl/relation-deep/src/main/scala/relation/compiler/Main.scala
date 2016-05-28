package relation
package compiler

import shallow._
import deep._

object Main extends App {
  
  implicit object Context extends RelationDSLOpsPackaged
  
  def pgrmA = dsl""" 
    val schema = Schema("number", "digit")
    val En = Relation.scan("data/En.csv", schema, "|")
    val selEn = En.select(x => x.getField(schema, "number") == "one")
    val projEn = selEn.project(Schema("number"))
    projEn.print
  """

  def pgrmB = dsl"""
    val EnSchema = Schema("number", "digit")
    val En = Relation.scan("data/En.csv", EnSchema, "|")
    val FrSchema = Schema("digit", "nombre")
    val Fr = Relation.scan("data/Fr.csv", FrSchema, "|")
    val EnFr = En.join(Fr, "digit", "digit")
    EnFr.print
  """
  
  def pgrmC = dsl"""
    val EnSchema = Schema("number", "digit")
    val En = Relation.scan("data/En.csv", EnSchema, "|")
    En.select(x => x.getField(EnSchema, "number") == "one")
    val projEn = En.project(Schema("number"))
    En.print
  """
  
  def lineItemSch = dsl"""Schema(
    "ORDERKEY",
    "PARTKEY",
    "SUPPKEY",
    "LINENUMBER",
    "QUANTITY",
    "EXTENDEDPRICE",
    "DISCOUNT",
    "TAX",
    "RETURNFLAG",
    "LINESTATUS",
    "SHIPDATE",
    "COMMITDATE",
    "RECEIPTDATE",
    "SHIPINSTRUCT",
    "SHIPMODE",
    "COMMENT0"
  )"""
  def lineItem = dsl"""Relation.scan("data/sf0.1/lineitem.tbl", $lineItemSch, "|")"""
  
  def partSuppSch = dsl"""Schema(
    "PARTKEY",
    "SUPPKEY",
    "AVAILQTY",
    "SUPPLYCOST",
    "COMMENT1"
  )"""
  def partSupp = dsl"""Relation.scan("data/sf0.1/partsupp.tbl", $partSuppSch, "|")"""
  
  def partSch = dsl"""Schema(
    "PARTKEY",
    "NAME",
    "MFGR",
    "BRAND",
    "TYPE",
    "SIZE",
    "CONTAINER",
    "RETAILPRICE",
    "COMMENT2"
  )"""
  def part = dsl"""Relation.scan("data/sf0.1/part.tbl", $partSch, "|")"""
  
  def ScanNPrint = dsl"""
    $part.print
  """
  def Project = dsl"""
    $part.project(Schema("NAME","MFGR","SIZE")).print
  """
  def Select = dsl"""
    $part.select(x => x.getField($partSch, "MFGR") == "Manufacturer#4").print
  """
  def Join = dsl"""
    $part.select(x => x.getField($partSch, "MFGR") == "Manufacturer#4")
      .join($partSupp.select(x => x.getField($partSuppSch, "SUPPKEY") == "42"), "PARTKEY", "PARTKEY").print
  """
  
  def Skew = dsl"""
    val A = Relation.scan("data/SkewA.csv", Schema("left", "middle"), "|")
    val B = Relation.scan("data/SkewB.csv", Schema("middle", "right"), "|")
    A.join(B, "middle", "middle").print
  """
  
  def pgrm = Join
  
  val compiler = new RelationCompiler(Context)

  compiler.compile(pgrm) 
}
