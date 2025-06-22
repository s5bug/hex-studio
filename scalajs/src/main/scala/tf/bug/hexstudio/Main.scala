package tf.bug.hexstudio

import calico.*
import calico.html.io.{*, given}
import calico.syntax.*
import cats.effect.*
import cats.syntax.all.*
import fs2.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlElement
import org.http4s.client.Client
import org.http4s.dom.FetchClientBuilder
import org.typelevel.log4cats.{Logger, LoggerFactory}
import org.typelevel.log4cats.console.ConsoleLoggerFactory
import scala.concurrent.duration.FiniteDuration
import tf.bug.hexstudio.number.HexxyApi
import tf.bug.hexstudio.pattern.PatternRegistry
import tf.bug.hexstudio.ui.{ControllerProgressBar, ProgramTab, ProgramUiState, Tab, TabsList, VmTab}

object Main extends IOWebApp {

  def httpClient: Resource[IO, Client[IO]] = FetchClientBuilder[IO].resource
  def loggerFactory: LoggerFactory[IO] = ConsoleLoggerFactory.create[IO]

  override def render: Resource[IO, HtmlElement[IO]] =
    for {
      http <- httpClient
      loggerFactory = Main.loggerFactory
      mainLogger <- loggerFactory.create.toResource

      programState <- ProgramUiState.empty.flatMap(SignallingRef[IO].of).toResource
      evaluator <- programState.discrete.switchMap { newState =>
        // TODO actually run the instructions
        Stream.never[IO]
      }.compile.drain.background

      selectedTab <- SignallingRef[IO].of[Tab](Tab.Program).toResource
      programTab <- ProgramTab(programState)
      vmTab <- VmTab()

      r <- app(http, loggerFactory, mainLogger, programState, selectedTab, programTab, vmTab)
    } yield r

  def app(
    http: Client[IO],
    loggerFactory: LoggerFactory[IO],
    mainLogger: Logger[IO],

    programState: SignallingRef[IO, ProgramUiState],

    selectedTab: SignallingRef[IO, Tab],
    programTab: HtmlElement[IO],
    vmTab: HtmlElement[IO]
  ): Resource[IO, HtmlElement[IO]] = {
    div(
      idAttr := "container",
      TabsList(selectedTab),
      div(
        idAttr := "toolbox",
        selectedTab.map {
          case Tab.Program => Resource.pure[IO, HtmlElement[IO]](programTab)
          case Tab.Vm => Resource.pure[IO, HtmlElement[IO]](vmTab)
        }
      ),
      div(
        idAttr := "renderer"
      ),
      div(
        idAttr := "controller",
        ControllerProgressBar(programState)
      )
    )
  }

}
