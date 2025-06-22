package tf.bug.hexstudio.iota.hexcasting

import tf.bug.hexstudio.iota.IotaType
import tf.bug.hexstudio.pattern.Pattern
import tf.bug.hexstudio.vm.PatternSource

object PatternIotaType extends IotaType {
  final case class Iota(
    pattern: Pattern,
    patternSource: PatternSource
  )
}
