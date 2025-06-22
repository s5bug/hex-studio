package tf.bug.hexstudio.ui

import cats.effect.IO
import fs2.concurrent.SignallingRef
import scala.collection.immutable.SortedMap
import tf.bug.hexstudio.iota.Iota
import tf.bug.hexstudio.pattern.Pattern
import tf.bug.hexstudio.vm.{PatternStep, SideEffect, VmInjectionRequest, VmWorldState}

final case class ProgramUiProgress(
  injectionRequests: SortedMap[VmInjectionRequest, Option[Iota]],
  vmStates: Vector[VmWorldState],
  patternSteps: Vector[PatternStep]
)

final case class ProgramUiState(
  instructions: Vector[Pattern],
  progress: SignallingRef[IO, ProgramUiProgress],
  lockedToEnd: Boolean,
  stepSelected: Int,
)

object ProgramUiState {
  def empty: IO[ProgramUiState] = for {
    progress <- SignallingRef.of[IO, ProgramUiProgress](ProgramUiProgress(
      injectionRequests = SortedMap(),
      vmStates = Vector(VmWorldState.empty),
      patternSteps = Vector()
    ))
  } yield ProgramUiState(
    instructions = Vector(),
    progress = progress,
    lockedToEnd = true,
    stepSelected = 0,
  )
}
