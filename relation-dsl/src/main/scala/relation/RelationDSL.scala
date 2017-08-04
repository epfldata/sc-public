package relation

import squid.ir._
import squid.lang.ScalaCore
//
//object RelationDSL extends SimpleANF with ScalaCore with Relation.Lang with Row.Lang with Schema.Lang with ClassEmbedder {
//  embed(Relation)
//}

object RelationDSL extends SimpleANF with ClassEmbedder with SimpleEffects with StandardEffects with OnlineOptimizer with CurryEncoding.ApplicationNormalizer {
  embed(Relation)
  embed(Stream)
}





