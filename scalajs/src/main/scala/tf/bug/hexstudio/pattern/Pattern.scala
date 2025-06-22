package tf.bug.hexstudio.pattern

import scala.quoted.Expr
import tf.bug.hexstudio.vm.{PatternStep, VmWorldState}

trait Pattern {
  def name: String
  def execute(vm: VmWorldState): PatternStep
  def registered: RegisteredPattern
}
