package tf.bug.hexstudio.ui

import calico.*
import calico.html.io.{*, given}
import calico.syntax.*
import cats.effect.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlElement

object TabsList {

  def apply(
    selectedTab: SignallingRef[IO, Tab]
  ): Resource[IO, HtmlElement[IO]] = div(
    idAttr := "tabs",
    div(
      cls := "tab-selector",
      onClick --> (_.foreach(e => selectedTab.set(Tab.Program))),
      h1("P")
    ),
    div(
      cls := "tab-selector",
      onClick --> (_.foreach(e => selectedTab.set(Tab.Vm))),
      h1("V")
    )
  )

}
