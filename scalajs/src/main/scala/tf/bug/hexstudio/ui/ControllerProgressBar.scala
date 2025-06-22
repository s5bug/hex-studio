package tf.bug.hexstudio.ui

import calico.*
import calico.html.io.{*, given}
import calico.frp.given
import calico.syntax.*
import cats.effect.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlElement

object ControllerProgressBar {

  def apply(programState: SignallingRef[IO, ProgramUiState]): Resource[IO, HtmlElement[IO]] = {
    inline def pStateSignal: Signal[IO, ProgramUiState] = programState
    
    div(
      idAttr := "progress-bar",
      input.withSelf { self => (
        `type` := "range",
        minAttr := "0",
        maxAttr <-- pStateSignal.flatMap(_.progress).map(_.vmStates.length - 1).map(_.toString),
        value <-- programState.map(_.stepSelected).map(_.toString),
        onInput --> (_.foreach(e => self.value.get.flatMap(v => programState.update(_.copy(stepSelected = v.toInt)))))
      )},
      input.withSelf { self => (
        `type` := "number",
        minAttr := "0",
        maxAttr <-- pStateSignal.flatMap(_.progress).map(_.vmStates.length - 1).map(_.toString),
        value <-- programState.map(_.stepSelected).map(_.toString),
        onInput --> (_.foreach(e => self.value.get.flatMap(v => programState.update(_.copy(stepSelected = v.toInt)))))
      )},
      "/",
      pStateSignal.flatMap(_.progress).map(_.vmStates.length - 1).map(_.toString)
    )
  }

}
