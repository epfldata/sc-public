package mylib.deep

import ch.epfl.data.sc.pardis.quasi.anf.{BaseQuasiExp, BaseQuasiExt}

class MyLibDSLOps extends MyLibOps with BaseQuasiExp

class MyLibDSLExtOps extends MyLibExtOps with BaseQuasiExt
