package mylib

import ch.epfl.data.sc.pardis.quasi.engine.QuasiAPI
import mylib.deep.{MyLibDSLExtOps, MyLibDSLOps}

package object compiler extends QuasiAPI[MyLibDSLOps, MyLibDSLExtOps]
