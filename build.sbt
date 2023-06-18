name := "sbt-coveralls"

import sbt.ScriptedPlugin.autoImport.scriptedLaunchOpts

import scala.sys.process._
lazy val generateXMLFiles =
  taskKey[Unit]("Generate XML files (for test)")
generateXMLFiles := {
  val log = streams.value.log
  s"./src/test/resources/generate.sh" ! log
}

lazy val prepareScripted =
  taskKey[Unit]("Update .git files to make scripted work")
prepareScripted := {
  val log = streams.value.log
  s"./src/sbt-test/prepare.sh" ! log
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
    scalaVersion := "2.12.17" // scala-steward:off
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
      "com.fasterxml.jackson.core" % "jackson-core" % "2.14.2",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.2",
      "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.0.202109080827-r",
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "io.circe" %% "circe-core" % "0.14.5",
      "io.circe" %% "circe-generic" % "0.14.5",
      "io.circe" %% "circe-parser" % "0.14.5",
      "org.mockito" % "mockito-core" % "5.4.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.15" % Test
    ),
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.sbtscoverage.version=" + "2.0.6",
      "-Dplugin.sbtcoveralls.version=" + version.value
    )
  )
