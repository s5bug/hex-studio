package tf.bug.hexstudio.antimirov

import java.lang
import scala.collection.immutable.NumericRange
import scala.collection.mutable

/**
 * Rx is a regular expression.
 */
sealed abstract class Rx { lhs =>

  import Rx.{Phi, Empty, Letter, Letters, Choice, Concat, Star, Repeat, Var}

  /**
   * Choice operator.
   *
   * The expression `x + y` (also written as `x | y`), means that
   * either `x` or `y` (but not both) will be used in a given
   * production. In regular expression syntax this is written as
   * 'x|y'. This is also sometimes known as alternation.
   *
   * The operator is called `+` because it corresponds to addition in
   * the Kleene algebra of regular expressions.
   */
  def +(rhs: Rx): Rx =
    if(this == rhs) this
    else Choice(this, rhs)

  /**
   * Alias for the choice operator.
   */
  def |(rhs: Rx): Rx =
    lhs + rhs

  /**
   * Concatenation operator.
   *
   * The expression `x * y` means that `x` and then `y` will be used
   * in a given production in that order. In regular expression syntax
   * this is written as 'xy'.
   *
   * The operator is called `*` because it corresponds to
   * multiplication in the Kleene algebra of regular expressions.
   */
  def *(rhs: Rx): Rx =
    if (rhs == Empty) this
    else Concat(this, rhs)

  /**
   * Kleene star operator.
   *
   * The expression `x.star` means that `x` will be applied
   * zero-or-more times. In regular expression syntax this would be
   * written as 'x*'.
   *
   * Kleene star satisfies the self-referential relation:
   *
   * x.star = Empty + (x * x.star)
   */
  def star: Rx = Star(this)

  /**
   * Kleene plus operator.
   *
   * The expression `x.plus` means that `x` will be applied
   * one-or-more times. In regular expression syntax this would be
   * written as 'x+'.
   *
   * Kleene plus is implemented in terms of Kleene star:
   *
   * x.plus = x * x.star
   *
   * Kleene plus also satisfies the self-referential relation:
   *
   * x.plus = x * (Empty + x.plus)
   */
  def plus: Rx =
    this * this.star

  /**
   * Optionality operator.
   *
   * The expression `x.optional` means that `x` will be applied
   * zero-or-one times. In regular expression syntax this would be
   * written as 'x?'.
   */
  def optional: Rx =
    this + Empty

  /**
   * Exponentiation operator.
   *
   * `x.pow(k)` is equivalent to `x * x *... * x` k times. This can be
   * written in regular expression syntax as `x{k}`.
   *
   * `x.pow(k)` is equivalent to `x.repeat(k, k)` in the same way that
   * 'x{k}' is equivalent to 'x{k,k}' in regular expression syntax.
   */
  def pow(k: Int): Rx =
    if (k <= 0) Rx.empty
    else this match {
      case Phi | Empty => this
      case _ => Repeat(this, k, k)
    }

  /**
   * Repetition operator.
   *
   * This repeats the given regex for at least `m` times and no more
   * than `n` times (so 0 <= m <= n). If n=0 this is equivalent to the
   * empty string.
   *
   * This method is equivalent to m concatenations followed by (n - m)
   * choice operations and concatenations, e.g.
   *
   * x{3,5} = xxx(|x(|x))
   */
  def repeat(m: Int, n: Int): Rx = {
    require(m >= 0, s"$m >= 0 was false")
    require(n >= m, s"$n >= $m was false")
    if (n == 0) Rx.empty
    else this match {
      case Phi | Empty => this
      case _ => Repeat(this, m, n)
    }
  }

  /**
   * Attempt to put a regular expression in a canonical form.
   *
   * This is not guaranteed to be a "minimal" form (in fact it will
   * often expand the size of the regex). However, two regular
   * exprssions that are equal should have equivalent representations
   * after canonicalization is performed.
   */
  def canonical: Rx =
    Rx.canonicalize(this)

  /**
   * Intersection operator.
   */
  def &(rhs: Rx): Rx =
    Rx.intersect(lhs, rhs)

  /**
   * Difference operator.
   */
  def -(rhs: Rx): Rx =
    Rx.difference(lhs, rhs)

  /**
   * Exclusive-or (XOR) operator.
   */
  def ^(rhs: Rx): Rx =
    Rx.xor(lhs, rhs)

  /**
   * Complement (negation) operator.
   */
  def unary_~ : Rx =
    Rx.difference(Rx.Universe, this)

  /**
   * Decide whether the regex accepts the string `s`.
   */
  def accepts(s: String): Boolean = {
    def recur(r: Rx, i: Int): Iterator[Unit] =
      if (i >= s.length) {
        if (r.acceptsEmpty) Iterator(()) else Iterator.empty
      } else {
        r.partialDeriv(s.charAt(i)).iterator.flatMap(recur(_, i + 1))
      }

    recur(this, 0).hasNext
  }

  /**
   * Decide whether the regex rejects the string `s`.
   */
  def rejects(s: String): Boolean =
    !accepts(s)

  /**
   * Set of "first characters" which this regex can accept.
   *
   * If a character is not in the firstSet, this regex cannot match
   * strings starting with that character.
   *
   * Each LetterSet represents one or more contiguous character
   * ranges. We represent the first set as a list of LetterSets to
   * ensure that each letter set is either completely valid, or
   * completely invalid, for every internal Rx value. For this regex,
   * we can treat each element of the list as a congruence class of
   * characters (which are all treated the same by this regex and all
   * its children). This is very important for efficiency.
   *
   * For example, the first set for the expression ([a-c]|[b-d])e*
   * would be List([a], [b-c], [d]). This is because [a] is only
   * matched by [a-c], [d] is only matched by [b-d], and [b-c] is
   * matched by both. If we instead returned something like
   * List([a-c], [d]), then [b-d] would partially (but not completely)
   * match [a-c].
   */
  lazy val firstSet: List[LetterSet] =
    this match {
      case Phi => Nil
      case Empty => Nil
      case Letter(c) => LetterSet(c) :: Nil
      case Letters(cs) => cs :: Nil
      case Choice(r1, r2) =>
        LetterSet.venn(r1.firstSet, r2.firstSet).map(_.value)
      case Concat(r1, r2) if r1.acceptsEmpty =>
        LetterSet.venn(r1.firstSet, r2.firstSet).map(_.value)
      case Concat(r1, _) => r1.firstSet
      case Star(r) => r.firstSet
      case Repeat(r, _, _) => r.firstSet
      // $COVERAGE-OFF$
      case Var(_) => sys.error("!")
      // $COVERAGE-ON$
    }

  /**
   * Compute the size of the set of strings matched by this
   * regular expression.
   *
   * In expressions using the Kleene star operator the cardinality
   * will often be unbounded, since the set is infinite (at least in
   * theory). To get a sense of the relative complexity of an
   * expression using Kleene stars, consider exploring how the values
   * of `limitStars(i).cardinality` increase as `i` does.
   *
   * This method traverses the expression to avoid double-counting the
   * same string which can be produced by two different paths
   */
  def cardinality: Size =
    Rx.cardinalityOf(this)

  /**
   * Limit applications of the Kleene star operator.
   *
   * Replaces `Star(r)` nodes with `Repeat(r, 0, count)` nodes. This
   * has the effect of limiting any Kleene star in the expression to
   * being expanded at most `count` times.
   */
  def limitStars(count: Int): Rx =
    this match {
      case Phi | Empty | Letter(_) | Letters(_) =>
        this
      case Choice(r1, r2) =>
        r1.limitStars(count) + r2.limitStars(count)
      case Concat(r1, r2) =>
        r1.limitStars(count) * r2.limitStars(count)
      case Star(r) =>
        r.limitStars(count).repeat(0, count)
      case Repeat(r, m, n) =>
        r.limitStars(count).repeat(m, n)
      case Var(_) =>
        // $COVERAGE-OFF$
        sys.error("!")
      // $COVERAGE-ON$
    }

  /**
   * The deepest number of nested Kleene star operators in this expression.
   *
   * This can be used as a complexity measure.
   */
  def starDepth: Int =
    this match {
      case Phi | Empty | Letter(_) | Letters(_) => 0
      case Choice(r1, r2) => Integer.max(r1.starDepth, r2.starDepth)
      case Concat(r1, r2) => Integer.max(r1.starDepth, r2.starDepth)
      case Star(r) => r.starDepth + 1
      case Repeat(r, _, _) => r.starDepth
      // $COVERAGE-OFF$
      case Var(_) => sys.error("!")
      // $COVERAGE-ON$
    }

  private val ReprLimit = Size(1000000000L)

  /**
   * Represent the cardinality of this expression as a string.
   *
   * For finite cardinalities, just return that number.
   *
   * For infinite star depths, we show ∞ and then display the first
   * cardinalities where we limit star expansions.
   */
  def cardRepr: String =
    cardinality match {
      case Size.Unbounded =>
        Iterator.from(0)
          .map(i => limitStars(i).cardinality)
          .zipWithIndex
          .takeWhile { case (n, i) => i < 6 && (n < ReprLimit || i < 3) }
          .map { case (n, _) => n.toString }
          .mkString("∞ (", ", ", ", ...)")
      case sz =>
        sz.approxString
    }

  /**
   * Return the range of string lenghts (if any) matched by this
   * regular expression.
   *
   * ϕ (or regular expressions equivalent to ϕ) will return None. All
   * other expressions will return Some((x, y)), where 0 <= x <= y < ∞.
   */
  lazy val matchSizes: Option[(Size, Size)] =
    this match {
      case Phi => None
      case Empty => Some((Size.Zero, Size.Zero))
      case Letter(_) | Letters(_) => Some((Size.One, Size.One))
      case Choice(r1, r2) =>
        (r1.matchSizes, r2.matchSizes) match {
          case (Some((x1, y1)), Some((x2, y2))) => Some((x1.min(x2), y1.max(y2)))
          case (some@Some(_), None) => some
          case (None, some@Some(_)) => some
          case (None, None) => None
        }
      case Concat(r1, r2) =>
        (r1.matchSizes, r2.matchSizes) match {
          case (Some((x1, y1)), Some((x2, y2))) =>
            Some((x1 + x2, y1 + y2))
          case _ =>
            None
        }
      case Star(r) =>
        Some((Size.Zero, r.matchSizes match {
          case Some((_, y)) => y * Size.Unbounded
          case None => Size.Zero
        }))
      case Repeat(r, m, n) =>
        r.matchSizes.map { case (x, y) =>
          (x * Size(m), y * Size(n))
        }
      // $COVERAGE-OFF$
      case Var(_) => sys.error("!")
      // $COVERAGE-ON$
    }

  /**
   * Do lhs and rhs accept the same set of strings?
   */
  def ===(rhs: Rx): Boolean =
    Rx.equiv(lhs, rhs)

  /**
   * Is lhs' accepted set a proper subset of rhs' accepted set?
   */
  def <(rhs: Rx): Boolean =
    partialCompare(rhs) < 0.0

  /**
   * Is lhs' accepted set a proper superset of rhs' accepted set?
   */
  def >(rhs: Rx): Boolean =
    partialCompare(rhs) > 0.0

  /**
   * Is lhs' accepted set a subset of rhs' accepted set?
   */
  def <=(rhs: Rx): Boolean =
    partialCompare(rhs) <= 0.0

  /**
   * Is lhs' accepted set a superset of rhs' accepted set?
   */
  def >=(rhs: Rx): Boolean =
    partialCompare(rhs) >= 0.0

  /**
   * Is lhs a subset of rhs?
   */
  def subsetOf(rhs: Rx): Boolean =
    partialCompare(rhs) <= 0.0

  /**
   * Is lhs a superset of rhs?
   */
  def supersetOf(rhs: Rx): Boolean =
    partialCompare(rhs) >= 0.0

  /**
   * Is lhs a proper subset of rhs?
   */
  def properSubsetOf(rhs: Rx): Boolean =
    partialCompare(rhs) < 0.0

  /**
   * Is lhs a proper superset of rhs?
   */
  def properSupersetOf(rhs: Rx): Boolean =
    partialCompare(rhs) > 0.0

  /**
   * String representation of Rx.
   */
  override def toString: String =
    reRepr

  /**
   * Return a list of nodes to be chosen between.
   *
   * If this node is a Choice node, the list will have two or more
   * elements. Otherwise it will have exactly one element.
   */
  def choices: Set[Rx] = Set(this)

  /**
   * Return a list of nodes to be concatenated.
   *
   * If this node is a Concat node, the list will have two or more
   * elements. Otherwise it will have exactly one element.
   */
  def concats: Vector[Rx] = Vector(this)

  /**
   * Represent this value using traditional "regex" syntax.
   *
   * The output of this method should be readable by Rx.parse.
   */
  def reRepr: String = {
    def recur(re: Rx, parens: Boolean): String =
      re match {
        case Phi => "∅"
        case Empty => ""
        case Var(x) => s"Var($x)"
        case Letter(c) => Chars.escape(c)
        case Letters(cs) => cs.toString
        case Star(r) => recur(r, true) + "*"
        case Repeat(r, m, n) =>
          val suffix = if (m == n) s"{$m}" else s"{$m,$n}"
          "(" + recur(r, true) + suffix + ")"
        case c@Choice(_, _) =>
          val s = c.choices.map(recur(_, false)).mkString("|")
          if (parens) s"($s)" else s
        case c@Concat(_, _) =>
          val s = c.concats.map(recur(_, true)).mkString
          if (parens) s"($s)" else s
      }

    recur(this, false)
  }

  /**
   * Represent this value using Scala syntax.
   *
   * The output of this method should be usable in the REPL.
   */
  def scalaRepr: String = {
    def recur(re: Rx): String =
      re match {
        case Phi => "ϕ"
        case Empty => "ε"
        case Letter(c) => s"Rx('${Chars.escape(c)}')"
        case Letters(cs) => "Rx.parse(\"" + cs.toString + "\")"
        case Choice(r1, r2) => s"(${recur(r1)}+${recur(r2)})"
        case Concat(r1, r2) => s"(${recur(r1)}*${recur(r2)})"
        case Star(r) => s"${recur(r)}.star"
        case Repeat(r, m, n) if m == n => s"${recur(r)}.repeat($m)"
        case Repeat(r, m, n) => s"${recur(r)}.repeat($m,$n)"
        case Var(x) => "$" + x.toString
      }

    recur(this)
  }

  /**
   * Represent this value using syntax passable to JS's RegExp constructor.
   */
  protected def jsReprInner(sb: lang.StringBuilder, parens: Boolean): Unit
  final def jsRepr: String = {
    val sb = new lang.StringBuilder()
    jsReprInner(sb, false)
    sb.toString
  }

  /**
   * Is this regular expressions equivalent to ϕ (phi)?
   */
  def isPhi: Boolean =
    this match {
      case Phi => true
      case Empty | Letter(_) | Letters(_) | Star(_) | Var(_) => false
      case Repeat(r, _, _) => r.isPhi
      case Choice(r1, r2) => r1.isPhi && r2.isPhi
      case Concat(r1, r2) => r1.isPhi || r2.isPhi
    }

  /**
   * Is this regular expression equivalent to ε (empty)?
   */
  def isEmpty: Boolean =
    this match {
      case Empty => true
      case Phi | Letter(_) | Letters(_) | Star(_) | Repeat(_, _, _) | Var(_) => false
      case Choice(r1, r2) => r1.isEmpty && r2.isEmpty
      case Concat(r1, r2) => r1.isEmpty && r2.isEmpty
    }

  /**
   * Does this regular expression accept the empty string ("")?
   */
  lazy val acceptsEmpty: Boolean =
    this match {
      case Empty | Star(_) => true
      case Phi | Letter(_) | Letters(_) => false
      case Repeat(r, m, _) => m == 0 || r.acceptsEmpty
      case Choice(r1, r2) => r1.acceptsEmpty || r2.acceptsEmpty
      case Concat(r1, r2) => r1.acceptsEmpty && r2.acceptsEmpty
      // $COVERAGE-OFF$
      case Var(_) => sys.error("!")
      // $COVERAGE-ON$
    }

  /**
   * Does this regular expression reject the empty string ("")?
   */
  def rejectsEmpty: Boolean =
    !acceptsEmpty

  /**
   * Compute the derivative with respect to `c`.
   *
   * The derivative of a regular expressions with respect to `c` is a
   * new regular expression that accepts a string `s` if and only if the
   * original expression accepts `c` + `s`.
   *
   * For example, the derivative of '(abc)*' with respect to 'a' is '(bc)(abc)*'
   */
  def deriv(c: Char): Rx =
    Rx.choice(partialDeriv(c))

  /**
   * Compute the partial derivatives with respect to `x`.
   *
   * A partial derivative of a regular expression with respect to `x`
   * is a new regular expression that only accepts a string `s` if the
   * original expression accepts `x` + `s`.
   *
   * Unlike a derivative, if a partial derivative does not match `s`
   * that does not mean that the original regular expression would not
   * match `x` + `s` -- there may be some other partial derivative
   * that does match `s`.
   *
   * If you calculate all the partial derivatives and union them
   * together, you get a regular expression that is equivalent to the
   * derivative. (In fact, this is how Antimirov implements the
   * derivative operation.)
   *
   * We use a Set here to avoid duplicate partial derivatives.
   */
  def partialDeriv(x: Char): Set[Rx] =
    this match {
      case Phi | Empty => Set.empty
      case Letter(c) if c == x => Set(Empty)
      case Letters(cs) if cs.contains(x) => Set(Empty)
      case Letter(_) | Letters(_) => Set.empty
      case Choice(r1, r2) => r1.partialDeriv(x) | r2.partialDeriv(x)
      case Star(r) => r.partialDeriv(x).filter(_ != Phi).map(_ * this)
      case Repeat(r, m, n) =>
        val s1 = r.partialDeriv(x).filter(_ != Phi)
        if (s1.isEmpty) {
          Set.empty
        } else {
          val rr = if (n <= 1) Empty else Repeat(r, Integer.max(0, m - 1), n - 1)
          s1.map(_ * rr)
        }
      case Concat(r1, r2) =>
        val s1 = r1.partialDeriv(x).map(_ * r2)
        if (r1.acceptsEmpty) s1 | r2.partialDeriv(x) else s1
      case Var(_) => sys.error("!")
    }

  /**
   * Rewrite regular expressions that have an embedded `Var(x)` node
   * into a new regular expression involving star.
   *
   * Var is used for self-referential formulas that are generated, and
   * which need to be implemented with Kleene star.
   *
   * For example, imagine that we have computed the following:
   *
   * R = x | (y * R)
   *
   * When matching with R, if we see x, we are done. However, if we
   * match y then we must match R. So we can only "finish" matching by
   * seeing an x, after seeing zero-or-more y's.
   *
   * Based on that description, notice that R corresponds to 'y*x'.
   *
   * Imagine we are computing the intersection (x & y). When we start
   * recursing on the derivatives of x and y, we keep track of the
   * fact that we're in the process of resolving (x & y) by adding (x,
   * y) to the environment, mapped to Var(1).
   *
   * If we encounter (x & y) again, we'll immediately replace that
   * with Var(1) to note that we've encountered a self-reference
   * (rather than continuing to infinitely recurse). Once we finish
   * the intersection, we need to rewrite these self-referential var
   * nodes into normal regex form (as we did with R).
   *
   * Every var node can be resolved into an expression that has the
   * following form:
   *
   * choice(r1, r2, ..., rn).star * choice(b1, b2, ..., bn)
   *
   * The inner method `recur` computes a list of rs and bs for each
   * node in the expression. Nodes that don't reference Var(x) will
   * only return elements in the bs. Var(x) (and expressions that
   * contain Var(x)) will put expressions in rs as well.
   */
  private def resolve(x: Int): Rx = {

    // cartesian product
    def cart(xs: Set[Rx], ys: Set[Rx]): Set[Rx] =
      for {x <- xs; y <- ys} yield x * y

    def recur(r: Rx, x: Int): (Set[Rx], Set[Rx]) =
      r match {
        case v@Var(y) =>
          if (y == x) (Set(Empty), Set.empty) else (Set.empty, Set(v))
        case Concat(r1, r2) =>
          val (rs1, bs1) = recur(r1, x)
          val (rs2, bs2) = recur(r2, x)
          (cart(rs1, rs2) | cart(rs1, bs2) | cart(bs1, rs2), cart(bs1, bs2))
        case Choice(r1, r2) =>
          val (rs1, bs1) = recur(r1, x)
          val (rs2, bs2) = recur(r2, x)
          (rs1 | rs2, bs1 | bs2)
        case r =>
          (Set.empty, Set(r))
      }

    val (rs, bs) = recur(this, x)
    Rx.choice(rs).star * Rx.choice(bs)
  }

  /**
   * Partial comparison using a subset relation between regular expressions.
   *
   * The return value represents the relationship:
   *
   * <0 means (lhs < rhs) means lhs is a subset of rhs
   * 0 means (lhs = rhs) means lhs is equivalent to rhs
   * >0 means (lhs > rhs) means lhs is a supserset of rhs
   * NaN means none of the above
   */
  def partialCompare(rhs: Rx): Double = {

    // operation table
    //    -1  0 +1 Na
    // -1 -1 -1 Na Na
    //  0 -1  0 +1 Na
    // +1 Na +1 +1 Na
    // Na Na Na Na Na
    def acc(x: Double, y: Double): Double =
      if (x == 0.0 || Math.signum(x) == Math.signum(y)) y
      else if (y == 0.0) x
      else Double.NaN

    val derivCache = mutable.Map.empty[(Rx, Char), Rx]

    def recur(env: Set[(Rx, Rx)], pair: (Rx, Rx)): Double =
      pair match {
        case (Phi, rhs) =>
          if (rhs.isPhi) 0.0 else -1.0
        case (_, Phi) =>
          1.0
        case (Empty, rhs) =>
          if (rhs.isEmpty) 0.0
          else if (rhs.acceptsEmpty) -1.0
          else Double.NaN
        case (lhs, Empty) =>
          if (lhs.acceptsEmpty) 1.0
          else Double.NaN
        case _ if env(pair) =>
          0.0
        case (lhs, rhs) =>

          var res = (lhs.acceptsEmpty, rhs.acceptsEmpty) match {
            case (false, true) => -1.0
            case (true, false) => 1.0
            case _ => 0.0
          }

          res = acc(res, Rx.rangeSubset(lhs.matchSizes, rhs.matchSizes))
          if (res.isNaN) return Double.NaN

          val alpha = LetterSet.venn(lhs.firstSet, rhs.firstSet)
          val diffIt = alpha.iterator
          while (diffIt.hasNext) {
            diffIt.next match {
              case Diff.Left(_) =>
                if (res < 0.0) return Double.NaN
                res = 1.0
              case Diff.Right(_) =>
                if (res > 0.0) return Double.NaN
                res = -1.0
              case _ => ()
            }
          }

          val env2 = env + pair
          val alphaIt = alpha.iterator
          while (alphaIt.hasNext && !res.isNaN) {
            val c = alphaIt.next.value.minOption.get
            val d1 = derivCache.getOrElseUpdate((lhs, c), lhs.deriv(c))
            val d2 = derivCache.getOrElseUpdate((rhs, c), rhs.deriv(c))
            val x = recur(env2, (d1, d2))
            res = acc(res, x)
          }
          res
      }

    if (lhs == rhs) 0.0 else recur(Set.empty, (lhs, rhs))
  }

  /**
   * Reverse the regular expression.
   *
   * For every string matched by the original expression, the reversed
   * expression matches the reversed string. There are no other
   * differences, and .reverse.reverse is a no-op.
   */
  def reverse: Rx =
    this match {
      case Concat(r1, r2) => Concat(r2.reverse, r1.reverse)
      case Choice(r1, r2) => Choice(r1.reverse, r2.reverse)
      case Star(r) => Star(r.reverse)
      case Repeat(r, m, n) => Repeat(r.reverse, m, n)
      case r => r
    }

}

object Rx {

  /**
   * Phi (ϕ) is the unmatchable regular expression.
   *
   * Phi corresponds to the 'zero' element of the Kleene algebra of
   * regular expressions:
   *
   * ϕ + x = x * ϕ = x
   * ϕ * x = x * ϕ = ϕ
   * ϕ.star = ε
   *
   * Its set of accepted strings is the empty set. Most regular
   * expression syntaxes do not support ϕ.
   */
  def phi: Rx =
    Phi

  /**
   * Empty (ε) is the empty regular expression.
   *
   * Empty corresponds to the 'one' element of the Kleene algebra of
   * regular expressions:
   *
   * ε * x = x * ε = x
   * ε.star = ε
   *
   * Its set of accepted strings contains only the empty string. Most
   * regular expression syntaxes denote this with an empty string,
   * for example in 'a(b|)'.
   */
  def empty: Rx =
    Empty

  /**
   * Dot (.) matches any single character.
   *
   * This is equivalent (and represented internally) as the character
   * group [\u0000-\uffff].
   */
  val dot: Rx =
    Rx.Letters(LetterSet.Full)

  /**
   * The regular expression that matches all possible strings.
   *
   * The expression for the universe is: '.*'
   */
  val Universe: Rx =
    closure(LetterSet.Full)

  /**
   * Parse the given string into a regular expression.
   *
   * The syntax used is a subset of the Java Regex syntax.
   */
  def parse(s: String): Rx =
    Parser.parse(s)

  /**
   * Construct a regular expression that matches the given character `c`.
   */
  def apply(c: Char): Rx =
    Letter(c)

  /**
   * Construct a regular expression that matches one character from
   * the given set `cs`.
   */
  def apply(cs: LetterSet): Rx =
    if (cs.isEmpty) Empty
    else cs.singleValue match {
      case Some(c) => Letter(c)
      case None => Letters(cs)
    }

  /**
   * Construct a regular expression that matches one character from
   * the given set `cs`.
   */
  def apply(cs: Set[Char]): Rx =
    if (cs.size == 0) Empty
    else if (cs.size == 1) Letter(cs.head)
    else Letters(LetterSet(cs))

  /**
   * Construct a regular expression that matches one character from
   * the given set `cs`, expressed as a range (e.g. c1 to c2).
   */
  def apply(cs: NumericRange[Char]): Rx =
    Rx(LetterSet(cs))

  /**
   * Construct a regular expression that matches the string `s`.
   */
  def apply(s: String): Rx =
    s.foldRight(Rx.empty)((c, r) => Letter(c) * r)

  /**
   * Construct a regular expression from the union of `rs`.
   *
   * If `rs` is empty, this method returns ϕ.
   */
  def choice(rs: Iterable[Rx]): Rx =
    rs.foldRight(phi)(_ + _)

  /**
   * Construct a regular expression from the concatenation of `rs`.
   *
   * If `rs` is empty, this method returns ε.
   */
  def concat(rs: Iterable[Rx]): Rx =
    rs.foldRight(empty)(_ * _)

  /**
   * Compute the closure for the given alphabet.
   *
   * The closure is the set of all possible strings using characters
   * from the alphabet. The closure of '.' is the universe.
   */
  def closure(alphabet: LetterSet): Rx =
    Letters(alphabet).star

  case object Phi extends Rx { // matches nothing
    override def +(rhs: Rx): Rx = rhs

    override def *(rhs: Rx): Rx = Phi

    override def star: Rx = Empty

    override def jsReprInner(sb: lang.StringBuilder, parens: Boolean): Unit =
      sb.append("[]")
  }

  case object Empty extends Rx { // matches empty string ("")
    override def +(rhs: Rx): Rx = rhs match {
      case Empty => Empty
      case _ => Choice(this, rhs)
    }

    override def *(rhs: Rx): Rx =
      if(rhs == Phi) Phi
      else rhs

    override def star: Rx = Empty

    override def jsReprInner(sb: lang.StringBuilder, parens: Boolean): Unit = ()
  }

  final case class Letter(c: Char) extends Rx { // single character
    override def +(rhs: Rx): Rx = rhs match {
      case this => this
      case Letter(c2) => Letters(LetterSet(c, c2))
      case Letters(cs) => Letters(cs + c)
      case _ => super.+(rhs)
    }

    override protected def jsReprInner(sb: lang.StringBuilder, parens: Boolean): Unit =
      sb.append(Chars.escape(c))
  }

  final case class Letters(ls: LetterSet) extends Rx { // single character, one of a set
    override def +(rhs: Rx): Rx = rhs match {
      case this => this
      case Letter(c) => Letters(ls + c)
      case Letters(cs) => Letters(ls | cs)
      case _ => super.+(rhs)
    }

    override protected def jsReprInner(sb: lang.StringBuilder, parens: Boolean): Unit =
      sb.append(ls.toString)
  }

  final case class Choice(r1: Rx, r2: Rx) extends Rx { // either
    override def choices: Set[Rx] = r1.choices | r2.choices

    override protected def jsReprInner(sb: lang.StringBuilder, parens: Boolean): Unit = {
      if(parens) sb.append("(?:")
      val iter = choices.iterator
      while(iter.hasNext) {
        val choice = iter.next()
        choice.jsReprInner(sb, false)
        if(iter.hasNext) sb.append("|")
      }
      if (parens) sb.append(")")
    }
  }

  final case class Concat(r1: Rx, r2: Rx) extends Rx { // concatenation
    override def concats: Vector[Rx] = r1.concats ++ r2.concats

    override protected def jsReprInner(sb: lang.StringBuilder, parens: Boolean): Unit = {
      if (parens) sb.append("(?:")
      val iter = concats.iterator
      while (iter.hasNext) {
        val concat = iter.next()
        concat.jsReprInner(sb, true)
      }
      if (parens) sb.append(")")
    }
  }

  final case class Repeat(r: Rx, m: Int, n: Int) extends Rx { // repetition, n > 0, n >= m
    override protected def jsReprInner(sb: lang.StringBuilder, parens: Boolean): Unit = {
      sb.append("(?:")
      r.jsReprInner(sb, true)
      sb.append("{")
      sb.append(m)
      if(m != n) {
        sb.append(",")
        sb.append(n)
      }
      sb.append("})")
    }
  }

  final case class Star(r: Rx) extends Rx { // kleene star
    override def star: Rx = this

    override protected def jsReprInner(sb: lang.StringBuilder, parens: Boolean): Unit = {
      r.jsReprInner(sb, true)
      sb.append("*")
    }
  }

  private[antimirov] final case class Var(x: Int) extends Rx { // used internally
    override protected def jsReprInner(sb: lang.StringBuilder, parens: Boolean): Unit = sys.error("!")
  }

  def cardinalityOf(r: Rx): Size = {
    val derivCache = mutable.Map.empty[(Rx, Char), Rx]

    def recur(r: Rx, seen: Set[Rx]): Size =
      r match {
        case Phi =>
          Size.Zero
        case Empty =>
          Size.One
        case Star(_) =>
          Size.Unbounded
        case _ if seen(r) =>
          Size.Unbounded
        case _ =>
          val seen2 = seen + r

          def f(cs: LetterSet): Size = {
            val c = cs.minOption.get
            val d = derivCache.getOrElseUpdate((r, c), r.deriv(c))
            Size(cs.size) * recur(d, seen2)
          }

          var total = Size.Zero
          val it = r.firstSet.iterator
          while (total.isFinite && it.hasNext) {
            total += f(it.next)
          }

          if (r.acceptsEmpty) total += Size.One
          total
      }

    recur(r, Set.empty)
  }

  def canonicalize(r: Rx): Rx = {
    val derivCache = mutable.Map.empty[(Rx, Char), Rx]

    def recur(cnt: Int, env: Map[Rx, Rx], r: Rx): Rx = {
      r match {
        case Phi => Phi
        case Empty => Empty
        case Star(r) => Star(r.canonical)
        case _ =>
          env.get(r) match {
            case Some(res) =>
              res
            case None =>
              val env2 = env.updated(r, Var(cnt))

              def f(cs: LetterSet): Rx = {
                val c = cs.minOption.get
                val d = derivCache.getOrElseUpdate((r, c), r.deriv(c))
                Rx(cs) * recur(cnt + 1, env2, d)
              }

              val set = r.firstSet.sortBy(s => (s.minOption, s.maxOption))
              val r1 = Rx.choice(set.map(f))
              val r2 = if (r.acceptsEmpty) r1 + Empty else r1
              r2.resolve(cnt)
          }
      }
    }

    recur(1, Map.empty, r)
  }

  /**
   * Semantic equivalence for regular expressions.
   */
  def equiv(lhs: Rx, rhs: Rx): Boolean = {
    val derivCache = mutable.Map.empty[(Rx, Char), Rx]

    def recur(env: Set[(Rx, Rx)], pair: (Rx, Rx)): Boolean = {
      pair match {
        case (r1, r2) if r1.acceptsEmpty != r2.acceptsEmpty => false
        case (r1, r2) if r1.isPhi != r2.isPhi => false
        case _ if env(pair) => true
        case (r1, r2) if r1.matchSizes != r2.matchSizes => false
        case (r1, r2) =>
          val env2 = env + pair
          val alpha = LetterSet.venn(r1.firstSet, r2.firstSet)
          alpha.forall(_.isBoth) && alpha.forall { d =>
            val c = d.value.minOption.get
            val d1 = derivCache.getOrElseUpdate((r1, c), r1.deriv(c))
            val d2 = derivCache.getOrElseUpdate((r2, c), r2.deriv(c))
            recur(env2, (d1, d2))
          }
      }
    }

    recur(Set.empty, (lhs, rhs))
  }

  def intersect(r1: Rx, r2: Rx): Rx = {
    val derivCache = mutable.Map.empty[(Rx, Char), Rx]

    def recur(cnt: Int, env: Map[(Rx, Rx), Rx], pair: (Rx, Rx)): Rx = {
      val (r1, r2) = pair
      pair match {
        case (Phi, _) | (_, Phi) => Phi
        case (Empty, r2) => if (r2.acceptsEmpty) Empty else Phi
        case (r1, Empty) => if (r1.acceptsEmpty) Empty else Phi
        case (r1, r2) =>
          env.get(pair) match {
            case Some(res) =>
              res
            case None =>
              val alpha = LetterSet.venn(r1.firstSet, r2.firstSet).collect {
                case Diff.Both(cs) => cs
              }
              val env2 = env.updated(pair, Var(cnt))

              def f(cs: LetterSet): Rx = {
                val c = cs.minOption.get
                val d1 = derivCache.getOrElseUpdate((r1, c), r1.deriv(c))
                val d2 = derivCache.getOrElseUpdate((r2, c), r2.deriv(c))
                Rx(cs) * recur(cnt + 1, env2, (d1, d2))
              }

              val rr = Rx.choice(alpha.map(f))
              val rr2 = if (r1.acceptsEmpty && r2.acceptsEmpty) rr + Empty else rr
              rr2.resolve(cnt)
          }
      }
    }

    recur(1, Map.empty, (r1, r2))
  }

  def difference(r1: Rx, r2: Rx): Rx = {
    val derivCache = mutable.Map.empty[(Rx, Char), Rx]

    def recur(cnt: Int, env: Map[(Rx, Rx), Rx], pair: (Rx, Rx)): Rx = {
      val (r1, r2) = pair
      pair match {
        case (Phi, _) => Phi
        case (Empty, r2) => if (r2.acceptsEmpty) Phi else Empty
        case (_, Phi) => r1
        case (r1, r2) =>
          env.get(pair) match {
            case Some(res) =>
              res
            case None =>
              val alpha = LetterSet.venn(r1.firstSet, r2.firstSet).collect {
                case Diff.Both(cs) => cs
                case Diff.Left(cs) => cs
              }
              val env2 = env.updated(pair, Var(cnt))

              def f(cs: LetterSet): Rx = {
                val c = cs.minOption.get
                val d1 = derivCache.getOrElseUpdate((r1, c), r1.deriv(c))
                val d2 = derivCache.getOrElseUpdate((r2, c), r2.deriv(c))
                Rx(cs) * recur(cnt + 1, env2, (d1, d2))
              }

              val rr = Rx.choice(alpha.map(f))
              val rr2 = if (r1.acceptsEmpty && r2.rejectsEmpty) rr + Empty else rr
              rr2.resolve(cnt)
          }
      }
    }

    recur(1, Map.empty, (r1, r2))
  }

  def xor(r1: Rx, r2: Rx): Rx = {
    val derivCache = mutable.Map.empty[(Rx, Char), Rx]

    def recur(cnt: Int, env: Map[(Rx, Rx), Rx], pair: (Rx, Rx)): Rx = {
      val (r1, r2) = pair
      pair match {
        case (r1, Phi) => r1
        case (Phi, r2) => r2
        case (Empty, r2) if r2.rejectsEmpty => r2 + Empty
        case (r1, Empty) if r1.rejectsEmpty => r1 + Empty
        case (r1, r2) =>
          env.get(pair) match {
            case Some(res) =>
              res
            case None =>
              val alpha = LetterSet.venn(r1.firstSet, r2.firstSet).map(_.value)
              val env2 = env.updated(pair, Var(cnt))

              def f(cs: LetterSet): Rx = {
                val c = cs.minOption.get
                val d1 = derivCache.getOrElseUpdate((r1, c), r1.deriv(c))
                val d2 = derivCache.getOrElseUpdate((r2, c), r2.deriv(c))
                Rx(cs) * recur(cnt + 1, env2, (d1, d2))
              }

              val rr = Rx.choice(alpha.map(f))
              val rr2 = if (r1.acceptsEmpty ^ r2.acceptsEmpty) rr + Empty else rr
              rr2.resolve(cnt)
          }
      }
    }

    recur(1, Map.empty, (r1, r2))
  }

  /**
   * Return whether lhs is an improper subset of rhs or not.
   *
   * We assume that x <= y for each tuple.
   */
  private def rangeSubset(lhs: Option[(Size, Size)], rhs: Option[(Size, Size)]): Double =
    (lhs, rhs) match {
      case _ if lhs == rhs => 0.0
      case (Some((x1, y1)), Some((x2, y2))) =>
        if (x2 <= x1 && y1 <= y2) -1.0
        else if (x1 <= x2 && y2 <= y1) 1.0
        else Double.NaN
      case _ => if (lhs.isEmpty) -1.0 else 1.0
    }

  /**
   * Pull literal strings out of a list of concatenations.
   */
  private[antimirov] def parseConcats(rs: List[Rx]): List[Either[String, Rx]] =
    rs match {
      case Nil =>
        Nil
      case Rx.Letter(c) :: rest =>
        parseConcats(rest) match {
          case Left(s) :: out => Left(c.toString + s) :: out
          case otherwise => Left(c.toString) :: otherwise
        }
      case r :: rest =>
        Right(r) :: parseConcats(rest)
    }


  /**
   * Lexicographic ordering for Rx.
   *
   * To compare two sets, the comparison considers the union of all
   * strings accepted by either in lexicographic ordering and finds
   * the "last" string (lexicographically) that is accepted by one but
   * not the other. The regex that doesn't accept this string comes
   * first.
   *
   * For example /b/ < /[ac]/, but /[bc]/ > /[ac]/.
   *
   * Facts about this ordering:
   *
   *     - Phi comes first and .* comes last
   *     - if x is a subset of y, then lexCompare(x, y) <= 0
   *     - if x is a superset of y, then lexCompare(x, y) >= 0
   *     - x <= y and y <= z implies x <= y
   */
  def lexCompare(lhs: Rx, rhs: Rx): Int = {
    val derivCache = mutable.Map.empty[(Rx, Char), Rx]

    def recur(env: Set[(Rx, Rx)], pair: (Rx, Rx)): Int =
      pair match {
        case (Phi, r2) => if (r2.isPhi) 0 else -1
        case (_, Phi) => 1
        case (Empty, Empty) => 0
        case _ if env(pair) => 0
        case (r1, r2) =>
          val env2 = env + pair
          val alpha = LetterSet.venn(r1.firstSet, r2.firstSet)
          var resc: Char = Char.MinValue
          var resn: Int =
            if (r1.acceptsEmpty) {
              if (r2.acceptsEmpty) 0 else -1
            }
            else if (r2.acceptsEmpty) 1 else 0
          val it = alpha.iterator
          while (it.hasNext) {
            it.next match {
              case Diff.Left(cs) =>
                val c = cs.maxOption.get
                if (resn == 0 || c >= resc) {
                  resc = c
                  resn = 1
                }
              case Diff.Right(cs) =>
                val c = cs.maxOption.get
                if (resn == 0 || c >= resc) {
                  resc = c
                  resn = -1
                }
              case Diff.Both(cs) =>
                val c = cs.maxOption.get
                if (resn == 0 || c >= resc) {
                  val d1 = derivCache.getOrElseUpdate((r1, c), r1.deriv(c))
                  val d2 = derivCache.getOrElseUpdate((r2, c), r2.deriv(c))
                  val n = recur(env2, (d1, d2))
                  if (n != 0) {
                    resc = c
                    resn = n
                  }
                }
            }
          }
          resn
      }

    recur(Set.empty, (lhs, rhs))
  }

  val lexicographicOrdering: Ordering[Rx] =
    (x: Rx, y: Rx) => Rx.lexCompare(x, y)

}
