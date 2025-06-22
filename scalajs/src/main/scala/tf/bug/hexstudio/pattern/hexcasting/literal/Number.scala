package tf.bug.hexstudio.pattern.hexcasting.literal

import tf.bug.hexstudio.antimirov.Rx
import tf.bug.hexstudio.pattern.{Pattern, RegisteredPattern}
import tf.bug.hexstudio.vm.{PatternStep, VmWorldState}

object Number extends RegisteredPattern {

  final case class Parsed(positive: Boolean, suffix: String) extends Pattern {
    lazy val value: Double = ???

    override def name: String = s"Numerical Reflection: $value"

    override def execute(vm: VmWorldState): PatternStep = ???

    override def registered: RegisteredPattern = Number
  }

  override def parse(signature: String): Parsed = signature match {
    case s"aaqa$positive" => Parsed(true, positive)
    case s"dedd$negative" => Parsed(false, negative)
  }

  override val pattern: Rx = Rx.parse("(aqaa|dedd)[qweasd]*")

}
