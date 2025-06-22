package tf.bug.hexstudio.pattern

import scala.quoted.{Expr, Quotes}
import tf.bug.hexstudio.antimirov.Rx

private[pattern] object PatternRegistryValidator {

  final case class FoldState(
    regexpGroups: List[String],
    unionedRxs: Rx,
    hasError: Boolean
  )

  inline def validate: scalajs.js.RegExp = ${ validateMacro }

  def validateMacro(using q: Quotes): Expr[scalajs.js.RegExp] = {
    val result = PatternRegistryDeferred.registry.foldLeft(FoldState(List.empty, Rx.Phi, false)) {
      case (FoldState(regexpGroups, unionedRxs, hasError), (id, pattern)) =>
        val checkIntersection = unionedRxs & pattern.pattern
        val errored = if(!checkIntersection.isPhi) {
          q.reflect.report.error(id + " overlaps with a different pattern")
          true
        } else false
        val nextUnion = unionedRxs | pattern.pattern
        val group = s"(?<${id.replace('/', '$')}>${pattern.pattern.jsRepr})"
        FoldState(group :: regexpGroups, nextUnion, hasError || errored)
    }
    if(result.hasError) q.reflect.report.errorAndAbort("errors in validating registry")

    val wholeString = result.regexpGroups.mkString("^(?:", "|", ")$")
    val wholeStringExpr = Expr(wholeString)
    '{ scalajs.js.RegExp.apply($wholeStringExpr) }
  }

}
