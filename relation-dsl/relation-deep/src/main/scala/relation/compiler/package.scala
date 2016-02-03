package relation

import ch.epfl.data.sc.pardis.quasi.engine.QuasiAPI
import relation.deep.{ RelationDSLOpsPackaged, RelationDSLExtOpsPackaged }

package object compiler extends QuasiAPI[RelationDSLOpsPackaged, RelationDSLExtOpsPackaged]
