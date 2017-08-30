import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

object Dependencies {
  val udashVersion = "0.5.0"
  val udashJQueryVersion = "1.0.1"
  val logbackVersion = "1.1.3"

  val deps = Def.setting(Seq[ModuleID](
    "io.udash" %%% "udash-core-frontend" % udashVersion,
    "io.udash" %%% "udash-jquery" % udashJQueryVersion
  ))

  val depsJS = Def.setting(Seq[org.scalajs.sbtplugin.JSModuleID](
  ))
}