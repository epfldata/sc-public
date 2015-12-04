package mylib
package shallow

import ch.epfl.data.sc.pardis.annotations._
import ch.epfl.data.sc.pardis.shallow.scalalib._

@deep
@deepExt
@needs[List[_] :: ScalaCore]
trait MyLib
