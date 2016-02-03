package relation
package shallow

import org.scalatest._
import Matchers._

class SimpleQuery extends FlatSpec {
  val RSchema = new Schema(List("number", "digit"))
  val RFile = "data/R.csv"
  val RScan = Relation.scan(RFile, RSchema, "|")

  "Scanning R" should "work" in {
    RScan.toString should be {
"""number, digit
one, 1
two, 2
three, 3
"""
    }
  }

  val RSelect = RScan.select(r => r.getField(RSchema, "digit").toInt % 2 == 1)

  "Selecting R" should "work" in {
    RSelect.toString should be {
"""number, digit
one, 1
three, 3
"""
    }
  }

  val RPSchema = new Schema(List("number"))
  val RProject = RScan.project(RPSchema)

  "Projecting R" should "work" in {
    RProject.toString should be {
"""number
one
two
three
"""
    }
  }
}
