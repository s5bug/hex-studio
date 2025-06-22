package tf.bug.hexstudio.vm

sealed trait PatternStep
object PatternStep {
  final case class Continue(
    resultVm: VmWorldState,
    mediaCost: Long,
    sideEffects: Vector[SideEffect],
    errors: Vector[String],
  )
  final case class Fatal(
    mediaCost: Long,
    sideEffects: Vector[SideEffect],
    cause: String
  )
  final case class InjectionRequired(
    vmInjectionRequest: VmInjectionRequest
  )
}
