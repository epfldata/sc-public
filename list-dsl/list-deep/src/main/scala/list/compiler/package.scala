package list

import ch.epfl.data.sc.pardis.quasi.engine.QuasiAPI
import list.deep.{ ListDSLOps, ListDSLExtOps }

package object compiler extends QuasiAPI[ListDSLOps, ListDSLExtOps]
