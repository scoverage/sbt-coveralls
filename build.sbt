name := "sbt-coveralls"

import sbt.ScriptedPlugin.autoImport.scriptedLaunchOpts

import scala.sys.process._
lazy val generateXMLFiles = taskKey[Unit]("Generate XML files (for test)")
generateXMLFiles := {
  val log = streams.value.log
  s"./src/test/resources/generate.sh" ! log
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
        "theon",
        "Ian Forsey",
        "",
        url("https://github.com/theon")
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
    scalaVersion := "2.12.16"
  )
)

lazy val root = Project("sbt-coveralls", file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    Test / publishArtifact := false,
    scalacOptions := Seq(
      "-unchecked",
      "-deprecation",
      "-feature",
      "-encoding", "utf8"
    ),
    dependencyOverrides ++= Seq(
      "com.jcraft" % "jsch" % "0.1.51"
    ),
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-core" % "2.13.4",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.1",
      "org.eclipse.jgit" % "org.eclipse.jgit" % "5.13.1.202206130422-r",
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "org.mockito" % "mockito-core" % "4.9.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.14" % Test
    ),
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value
    )
  )
