package mylib

import ch.epfl.data.sc.pardis.quasi.engine.QuasiAPI
import mylib.deep.{MyLibDSLExt, MyLibDSL}

package object compiler extends QuasiAPI[MyLibDSL, MyLibDSLExt]


