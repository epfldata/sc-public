package ch.epfl.data
package vector

import scala.reflect.macros.blackbox.Context
import scala.language.experimental.macros
import ch.epfl.yinyang._
import ch.epfl.yinyang.typetransformers._
import sc.pardis.ir._

package object shadow {
  def dsl[T](block: => T): Expression[T] = macro _dsl[T]

  def _dsl[T](c: Context)(block: c.Expr[T]): c.Expr[Expression[T]] =
    YYTransformer[c.type, T](c)(
      "ch.epfl.data.vector.deep.VectorYY",
      new PardisRepTransformer[c.type](c),
      None,
      None,
      Map("shallow" -> false, "debug" -> 0, "featureAnalysing" -> false, "virtualizeFunctions" -> true, "ascriptionTransforming" -> true, "virtualizeVal" -> false))(block).asInstanceOf[c.Expr[Expression[T]]]

  def dslOpt[T](block: => T): Expression[T] = macro _dslOpt[T]

  def _dslOpt[T](c: Context)(block: c.Expr[T]): c.Expr[Expression[T]] =
    YYTransformer[c.type, T](c)(
      "ch.epfl.data.vector.deep.VectorYYOpt",
      new PardisRepTransformer[c.type](c),
      None,
      None,
      Map("shallow" -> false, "debug" -> 0, "featureAnalysing" -> false, "virtualizeFunctions" -> true, "ascriptionTransforming" -> true, "virtualizeVal" -> false))(block).asInstanceOf[c.Expr[Expression[T]]]
}
