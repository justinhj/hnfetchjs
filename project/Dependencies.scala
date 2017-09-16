import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

object Dependencies {
  val udashVersion = "0.5.0"
  val udashJQueryVersion = "1.0.1"
  val logbackVersion = "1.1.3"

  val deps = Def.setting(Seq[ModuleID](
    "io.udash" %%% "udash-core-frontend" % udashVersion,
    "io.udash" %%% "udash-jquery" % udashJQueryVersion,
    "io.udash" %%% "udash-bootstrap" % udashVersion,
    "com.47deg" %%% "fetch" % "0.6.3",
    "com.lihaoyi" %%% "upickle" % "0.4.4",
    "org.typelevel" %%% "cats" % "0.9.0"
  ))

  val depsJS = Def.setting(Seq[org.scalajs.sbtplugin.JSModuleID](
  ))
}
