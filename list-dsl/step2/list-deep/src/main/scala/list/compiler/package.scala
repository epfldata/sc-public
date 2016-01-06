package list

import ch.epfl.data.sc.pardis.quasi.engine.QuasiAPI
import list.deep.{ ListDSLOpsPackaged, ListDSLExtOpsPackaged }

package object compiler extends QuasiAPI[ListDSLOpsPackaged, ListDSLExtOpsPackaged]
