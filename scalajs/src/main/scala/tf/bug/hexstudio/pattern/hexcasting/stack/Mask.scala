package tf.bug.hexstudio.pattern.hexcasting.stack

import scala.annotation.switch
import tf.bug.hexstudio.antimirov.Rx
import tf.bug.hexstudio.pattern.{Pattern, RegisteredPattern}
import tf.bug.hexstudio.vm.{PatternStep, VmWorldState}

object Mask extends RegisteredPattern {

  final case class Parsed(indicesToKeep: Vector[Boolean]) extends Pattern {
    override lazy val name: String = "Bookkeeper's Gambit: " ++ indicesToKeep.map {
      case true => "-"
      case false => "v"
    }.mkString

    override def execute(vm: VmWorldState): PatternStep = ???

    override def registered: RegisteredPattern = Mask
  }

  override def parse(signature: String): Parsed = signature match {
    case "" => Parsed(Vector(true))
    case "a" => Parsed(Vector(false))
    case _ =>
      val builder = Vector.newBuilder[Boolean]
      var idx = 0
      // https://discord.com/channels/936370934292549712/950847275549229086/1156756695217881130
      // 0 => start
      // 1 => straight
      // 2 => spike down
      // 3 => spike up
      var state = 0
      while(idx < signature.length) {
        (state: @switch) match {
          case 0 => signature.charAt(idx) match {
            case 'w' =>
              builder.addOne(true)
              builder.addOne(true)
              state = 1
            case 'e' =>
              builder.addOne(true)
              state = 3
            case 'a' =>
              builder.addOne(false)
              state = 2
          }
          case 1 => signature.charAt(idx) match {
            case 'w' =>
              builder.addOne(true)
            case 'e' =>
              state = 3
          }
          case 2 => signature.charAt(idx) match {
            case 'e' =>
              builder.addOne(true)
              state = 1
            case 'd' =>
              state = 3
          }
          case 3 => signature.charAt(idx) match {
            case 'a' =>
              builder.addOne(false)
              state = 2
          }
        }
        idx += 1
      }
      Parsed(builder.result())
  }

  override val pattern: Rx = Rx.parse("(w*ea|a)(da|ew*ea)*(ew*)?|w*")

}
