package tf.bug.hexstudio.ui

import calico.*
import calico.html.io.{*, given}
import calico.syntax.*
import cats.effect.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.{Signal, SignallingRef}
import fs2.dom.HtmlElement

object ControllerProgressBar {

  def apply(programState: SignallingRef[IO, ProgramUiState]): Resource[IO, HtmlElement[IO]] = {
    SignallingRef[IO].of(0).toResource.flatMap { maxVmState =>
      programState.discrete.flatMap(_.progress.discrete).map(_.vmStates.length - 1)
        .foreach(maxVmState.set)
        .compile.drain.background.flatMap { _ =>
          div(
            idAttr := "progress-bar",
            input.withSelf { self =>
              (
                `type` := "range",
                idAttr := "progress-bar-range",
                minAttr := "0",
                maxAttr <-- maxVmState.map(_.toString),
                value <-- programState.map(_.stepSelected).map(_.toString),
                onChange --> (_.foreach(e => self.value.get.flatMap(v => programState.update(_.copy(stepSelected = v.toInt)))))
              )
            },
            input.withSelf { self =>
              (
                `type` := "number",
                minAttr := "0",
                maxAttr <-- maxVmState.map(_.toString),
                value <-- programState.map(_.stepSelected).map(_.toString),
                onChange --> (_.foreach(e => self.value.get.flatMap(v => programState.update(_.copy(stepSelected = v.toInt)))))
              )
            },
            "/",
            maxVmState.map(_.toString)
          )
        }
    }
  }

}
