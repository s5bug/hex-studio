package tf.bug.hexstudio.ui

sealed trait Tab
object Tab {
  case object Program extends Tab
  case object Vm extends Tab
}
