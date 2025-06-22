package tf.bug.hexstudio.pattern

import tf.bug.hexstudio.antimirov.Rx

abstract class RegisteredPattern {
  val pattern: Rx

  type Parsed <: Pattern
  def parse(signature: String): Parsed
}
