package tf.bug.hexstudio.number

object Squiggler {

  val squiggleMapping: Map[String, Int] = Map(
    // for the first group, which will only have one bit
    "0" -> 2,
    "1" -> 6,
    // all the rest of the groups
    "000" -> 2,
    "001" -> 3,
    "010" -> 4,
    "011" -> 5,
    "100" -> 6,
    "101" -> 7,
    "110" -> 8,
    "111" -> 9
  )

  def squiggles(d: Double): Vector[Int] = {
    val longBits = java.lang.Double.doubleToLongBits(d)
    val binString = longBits.toBinaryString
    val mantissa = binString.takeRight(52)
    val vecSlices = mantissa.grouped(3).toVector

    // the least significant bits are the most significant squiggles
    val reorder = vecSlices.reverse
    reorder.map(squiggleMapping)
  }

  def evaluateSquiggles(squiggles: Vector[Int]): Double = {
    val start = ((((1.0d) + 10.0d) / 2.0d) / 2.0d)

    def applySquiggle(start: Double, len: Int): Double = {
      val setup = start + 1.0d
      val squiggle = (0 until len).foldLeft(setup)((acc, _) => ((acc * 2.0d) / 2.0d))
      val turn = squiggle / 2.0d
      val back = (0 until len).foldLeft(turn)((acc, _) => acc + 1.0d)
      ((back + 1) / 2.0d) / 2.0d
    }

    (squiggles.foldLeft(start)(applySquiggle)) + 1.0d
  }

  def drawSquiggles(sign: Int, squiggles: Vector[Int]): String = {
    val sb: java.lang.StringBuilder = new java.lang.StringBuilder()

    if (sign >= 0) sb.append("aqaa")
    else sb.append("dedd")

    sb.append("wedd")

    def applySquiggle(len: Int): Unit = {
      sb.append('w')
      (0 until len).foreach(_ => sb.append("ad"))
      sb.append('d')
      (0 until len).foreach(_ => sb.append('w'))
      sb.append("wdd")
    }

    squiggles.foreach(applySquiggle)

    sb.append('w')

    sb.toString
  }

  def exponent(target: Double): Int = {
    val bits: Long = java.lang.Double.doubleToLongBits(target)
    val exponentMask: Long = 0x7ff0_0000_0000_0000L
    val exponent = (bits & exponentMask) >>> 52
    (exponent.toInt - 1023)
  }

  def exponentDiff(target: Double, actual: Double): Int = {
    exponent(target) - exponent(actual)
  }

  def drawExponentDiff(diff: Int): String = {
    if (diff > 0) {
      (1 to diff).reverse.map(l => "da".repeat(l) ++ "a").mkString
    } else if (diff < 0) {
      (0 until (-diff)).reverse.map(l => "da".repeat(l) ++ "d").mkString
    } else ""
  }

  def numberLiteral(target: Double): String = {
    if (target == 0.0d) "aqaa"
    else if (target == -0.0d) "dedd"
    else {
      val (sign, realTarget) =
        if (target >= 0.0) (1, target)
        else (-1, -target)

      val squiggles = Squiggler.squiggles(realTarget)
      val actual = evaluateSquiggles(squiggles)
      val diff = exponentDiff(target, actual)

      drawSquiggles(sign, squiggles) ++ drawExponentDiff(diff)
    }
  }

}
