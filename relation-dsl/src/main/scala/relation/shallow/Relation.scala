package relation
package shallow

import ch.epfl.data.sc.pardis.annotations._

class Record

class Schema

@deep
@needs[(_,_) :: Seq[_]]
class Relation {
  def select(p: Record => Boolean): Relation = ???
  def project(schema: Schema): Relation = ???
  def join(o: Relation, cond: (Record, Record) => Boolean): Relation = ???
  def aggregate(key: Schema, agg: (Double, Record) => Double): Relation = ???
  def print: Unit = ???
}
