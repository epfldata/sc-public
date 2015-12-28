package list
package shallow

import ch.epfl.data.sc.pardis.annotations._
import ch.epfl.data.sc.pardis.shallow.scalalib._

@language
@deep
@quasiquotation
@needs[List[_] :: Mem :: ScalaCore]
trait ListDSL
