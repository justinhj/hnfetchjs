import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

object Dependencies {
  val udashVersion = "0.6.3-SNAPSHOT"
  val udashJQueryVersion = "1.0.1"
  val logbackVersion = "1.1.3"

  val deps = Def.setting(Seq[ModuleID](
    "io.udash" %%% "udash-core-frontend" % udashVersion,
    "io.udash" %%% "udash-jquery" % udashJQueryVersion,
    "io.udash" %%% "udash-bootstrap" % udashVersion,
    "com.github.japgolly.scalacss" %%% "core" % "0.5.3",
    "com.github.japgolly.scalacss" %%% "ext-scalatags" % "0.5.3",
    "com.47deg" %%% "fetch" % "0.6.3",
    "com.lihaoyi" %%% "upickle" % "0.4.4",
    "org.typelevel" %%% "cats" % "0.9.0",
    "ru.pavkin" %%% "scala-js-momentjs" % "0.9.0",
    "org.stanch" %%% "reftree" % "1.1.3"

  ))

  val depsJS = Def.setting(Seq[org.scalajs.sbtplugin.JSModuleID](
  ))
}
