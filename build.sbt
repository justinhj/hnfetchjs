import com.lihaoyi.workbench.Plugin._
import UdashBuild._
import Dependencies._

name := "hnfetchsjs"

version in ThisBuild := "0.1.0-SNAPSHOT"
scalaVersion in ThisBuild := "2.12.2"
organization in ThisBuild := "com.justinhj"
crossPaths in ThisBuild := false
scalacOptions in ThisBuild ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:implicitConversions",
  "-language:existentials",
  "-language:dynamics",
  "-Xfuture",
  "-Xfatal-warnings",
  "-Xlint:-unused,_"
)

val hnfetchsjs = project.in(file(".")).enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= deps.value,
    jsDependencies ++= depsJS.value,
    scalaJSUseMainModuleInitializer in Compile := true,

    compile := (compile in Compile).dependsOn(compileStatics).value,
    compileStatics := {
      IO.copyDirectory(sourceDirectory.value / "main/assets/fonts", crossTarget.value / StaticFilesDir / WebContent / "assets/fonts")
      IO.copyDirectory(sourceDirectory.value / "main/assets/images", crossTarget.value / StaticFilesDir / WebContent / "assets/images")
      IO.copyDirectory(sourceDirectory.value / "main/assets/bootstrap", crossTarget.value / StaticFilesDir / WebContent / "assets/bootstrap")
      IO.copyDirectory(sourceDirectory.value / "main/assets/js", crossTarget.value / StaticFilesDir / WebContent / "assets/js")
      val statics = compileStaticsForRelease.value
      (crossTarget.value / StaticFilesDir).***.get
    },

    artifactPath in(Compile, fastOptJS) :=
      (crossTarget in(Compile, fastOptJS)).value / StaticFilesDir / WebContent / "scripts" / "frontend-impl-fast.js",
    artifactPath in(Compile, fullOptJS) :=
      (crossTarget in(Compile, fullOptJS)).value / StaticFilesDir / WebContent / "scripts" / "frontend-impl.js",
    artifactPath in(Compile, packageJSDependencies) :=
      (crossTarget in(Compile, packageJSDependencies)).value / StaticFilesDir / WebContent / "scripts" / "frontend-deps-fast.js",
    artifactPath in(Compile, packageMinifiedJSDependencies) :=
      (crossTarget in(Compile, packageMinifiedJSDependencies)).value / StaticFilesDir / WebContent / "scripts" / "frontend-deps.js"
  ).settings(workbenchSettings:_*)
  .settings(
    bootSnippet := "com.justinhj.Init().main();",
    updatedJS := {
      var files: List[String] = Nil
      ((crossTarget in Compile).value / StaticFilesDir ** "*.js").get.foreach {
        (x: File) =>
          streams.value.log.info("workbench: Checking " + x.getName)
          FileFunction.cached(streams.value.cacheDirectory / x.getName, FilesInfo.lastModified, FilesInfo.lastModified) {
            (f: Set[File]) =>
              val fsPath = f.head.getAbsolutePath.drop(new File("").getAbsolutePath.length)
              files = "http://localhost:12345" + fsPath :: files
              f
          }(Set(x))
      }
      files
    },
    //// use either refreshBrowsers OR updateBrowsers
    // refreshBrowsers := (refreshBrowsers triggeredBy (compileStatics in Compile)).value
    updateBrowsers := (updateBrowsers triggeredBy (compileStatics in Compile)).value
  )

  