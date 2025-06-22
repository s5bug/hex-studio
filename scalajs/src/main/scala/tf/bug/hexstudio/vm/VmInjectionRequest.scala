package tf.bug.hexstudio.vm

final case class VmInjectionRequest(
  source: PatternSource,
)

object VmInjectionRequest {
  // TODO sort by order executed and then by order requested
  given sortByOrderExecuted: Ordering[VmInjectionRequest] =
    Ordering.by(_.source.executionIndex)
}
