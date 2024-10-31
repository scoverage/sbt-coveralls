name := "sbt-coveralls"

import sbt.ScriptedPlugin.autoImport.scriptedLaunchOpts

import java.io.File
import scala.sys.process._
lazy val generateXMLFiles =
  taskKey[Unit]("Generate XML files (for test)")
generateXMLFiles := {
  val dir = (Test / resourceDirectory).value
  val pwd = (run / baseDirectory).value

  val template =
    if (System.getProperty("os.name").startsWith("Windows"))
      ".xml.windows.template"
    else
      ".xml.template"

  dir.listFiles { (_, name) => name.endsWith(template) }.foreach {
    templateFile =>
      val newFile = dir / templateFile.getName.replace(template, ".xml")
      val content = IO.read(templateFile)
      IO.write(newFile, content.replace("{{PWD}}", pwd.absolutePath))
  }
}

lazy val prepareScripted =
  taskKey[Unit]("Update .git files to make scripted work")
prepareScripted := {
  val log = streams.value.log
  val pwd = (run / baseDirectory).value

  val submodules = "git submodule status" !! log
  val submodulePaths = submodules.split('\n').map { x =>
    x.split(" ")(2)
  }

  submodulePaths.foreach { subModulePath =>
    val path = pwd / ".git" / "modules" / subModulePath
    val pathFixedForWindows =
      if (System.getProperty("os.name").startsWith("Windows"))
        path.absolutePath.replace(
          File.separator,
          "/"
        ) // Git under Windows uses / for path separator
      else
        path.absolutePath
    val destination = file(subModulePath) / ".git"
    IO.delete(destination)
    IO.write(destination, s"gitdir: $pathFixedForWindows")
  }
}

inThisBuild(
  List(
    organization := "org.scoverage",
    homepage := Some(url("http://scoverage.org")),
    developers := List(
      Developer(
        "sksamuel",
        "Stephen Samuel",
        "sam@sksamuel.com",
        url("https://github.com/sksamuel")
      ),
      Developer(
        "rolandtritsch",
        "Roland Tritsch",
        "roland@tritsch.email",
        url("https://github.com/rolandtritsch")
      )
    ),
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/license/LICENSE-2.0")
    ),
    scalaVersion := "2.12.17", // scala-steward:off
    versionScheme := Some("semver-spec")
  )
)

lazy val root = Project("sbt-coveralls", file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    Test / publishArtifact := false,
    scalacOptions := Seq(
      "-release:8",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-encoding",
      "utf8"
    ),
    dependencyOverrides ++= Seq(
      "com.jcraft" % "jsch" % "0.1.55"
    ),
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.18.1",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.18.0",
      "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.0.202109080827-r",
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "io.circe" %% "circe-core" % "0.14.10",
      "io.circe" %% "circe-generic" % "0.14.10",
      "io.circe" %% "circe-parser" % "0.14.10",
      "org.mockito" % "mockito-core" % "5.14.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    ),
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.sbtscoverage.version=" + "2.0.9",
      "-Dplugin.sbtcoveralls.version=" + version.value
    )
  )
