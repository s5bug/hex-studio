package tf.bug.hexstudio.vm

import scala.collection.immutable.SortedMap
import tf.bug.hexstudio.iota.Iota
import tf.bug.hexstudio.pattern.Pattern

final case class VmWorldState(
  backtrace: Vector[Pattern],
  stack: Vector[Iota],
  introspectionDepth: Int,
  introspected: Vector[Iota],
  sideEffects: Vector[SideEffect],
)

object VmWorldState {
  def empty: VmWorldState = VmWorldState(
    backtrace = Vector(),
    stack = Vector(),
    introspectionDepth = 0,
    introspected = Vector(),
    sideEffects = Vector()
  )
}
