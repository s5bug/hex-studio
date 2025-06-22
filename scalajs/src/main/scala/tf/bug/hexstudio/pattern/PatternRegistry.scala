package tf.bug.hexstudio.pattern

object PatternRegistry {

  final def registry: Map[String, RegisteredPattern] = PatternRegistryDeferred.registry
  final val matchInput: scalajs.js.RegExp = PatternRegistryValidator.validate

}
