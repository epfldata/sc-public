package mylib.deep

import ch.epfl.data.sc.pardis.deep.{DSLExtOpsClass, DSLExpOpsClass}
import mylib.compiler.Optim

class MyLibDSL extends DSLExpOpsClass with ListOps with Optim.Online

class MyLibDSLExt extends DSLExtOpsClass with ListExtOps

