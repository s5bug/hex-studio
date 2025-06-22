import org.scalajs.linker.interface.{ESVersion, ModuleInitializer, ModuleSplitStyle}

lazy val hexstudio = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    organization := "tf.bug",
    name := "hexstudio",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "3.7.1",
    scalaJSUseMainModuleInitializer := false,
    Compile / scalaJSModuleInitializers ++= Seq(
      ModuleInitializer.mainMethodWithArgs("tf.bug.hexstudio.Main", "main").withModuleID("main"),
    ),
    scalaJSLinkerConfig ~= {
      _.withESFeatures(_.withESVersion(ESVersion.ES2020))
        .withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("tf.bug")))
    },
    scalacOptions ++= Seq(
      "-no-indent",
    ),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "org.typelevel" %%% "cats-core" % "2.13.0",
      "org.typelevel" %%% "cats-effect" % "3.6.1",
      "co.fs2" %%% "fs2-core" % "3.12.0",
      "com.armanbilge" %%% "calico" % "0.2.3",
      "org.http4s" %%% "http4s-client" % "0.23.30",
      "org.http4s" %%% "http4s-dom" % "0.2.12",
      "org.typelevel" %%% "log4cats-core" % "2.7.1",
      "org.typelevel" %%% "log4cats-js-console" % "2.7.1",
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core" % "2.36.5",
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-macros" % "2.36.5" % "compile-internal",
      "org.typelevel" %%% "cats-parse" % "1.1.0",
    ),
)
