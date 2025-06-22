package tf.bug.hexstudio.pattern

import tf.bug.hexstudio.pattern.hexcasting.literal.Number
import tf.bug.hexstudio.pattern.hexcasting.math.Add

private[pattern] object PatternRegistryDeferred {

  final val registry: Map[String, RegisteredPattern] = Map(
    "add" -> Add,
    "number" -> Number
  )

}
