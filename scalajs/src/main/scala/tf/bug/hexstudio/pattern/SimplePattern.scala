package tf.bug.hexstudio.pattern

import tf.bug.hexstudio.antimirov.Rx

abstract class SimplePattern(_pattern: String) extends RegisteredPattern with Pattern {

  override val pattern: Rx = Rx(_pattern)

  override final type Parsed = this.type
  override final def parse(signature: String): this.type = this
  override final def registered: RegisteredPattern = this

}
