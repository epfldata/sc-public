package mylib
package shallow

import ch.epfl.data.sc.pardis.annotations._
import ch.epfl.data.sc.pardis.shallow.scalalib._

@quasiquotation
@deep
@needs[List[_] :: Mem :: ScalaCore]
trait MyLibDSL
