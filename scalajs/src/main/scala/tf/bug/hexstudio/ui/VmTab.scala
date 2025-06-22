package tf.bug.hexstudio.ui

import calico.*
import calico.html.io.{*, given}
import calico.syntax.*
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlElement

object VmTab {

  def apply(): Resource[IO, HtmlElement[IO]] = div(
    cls := "tab",
    h2("VM")
  )

}
