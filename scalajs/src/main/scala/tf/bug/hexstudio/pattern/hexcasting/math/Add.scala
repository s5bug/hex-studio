package tf.bug.hexstudio.pattern.hexcasting.math

import tf.bug.hexstudio.pattern.SimplePattern
import tf.bug.hexstudio.vm.{PatternStep, VmWorldState}

object Add extends SimplePattern("waaw") {

  override def name: String = "Additive Distillation"

  override def execute(vm: VmWorldState): PatternStep = ???
  
}
