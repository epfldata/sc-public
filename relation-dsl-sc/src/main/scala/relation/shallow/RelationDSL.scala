package relation
package shallow

import ch.epfl.data.sc.pardis.annotations._
import ch.epfl.data.sc.pardis.shallow.scalalib._

class RelationDSLExtra

@language
@deep
@quasiquotation
@needs[Relation :: RelationScanner :: Array[_] :: ScalaCore :: RelationDSLExtra]
trait RelationDSL
