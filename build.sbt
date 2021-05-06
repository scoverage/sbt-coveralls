name := "sbt-coveralls"

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
      )
    ),
    licenses := Seq(
      "Apache-2.0" -> url("http://www.apache.org/license/LICENSE-2.0")
    ),
    scalaVersion := "2.12.13"
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
      "-encoding",
      "utf8"
    ),
    dependencyOverrides ++= Seq(
      "com.jcraft" % "jsch" % "0.1.51"
    ),
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-core" % "2.9.0",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.0",
      // DO NOT UPGRADE: later versions of jgit use Java 7 and we still need to support Java 6
      "org.eclipse.jgit" % "org.eclipse.jgit" % "3.7.0.201502260915-r",
      //"org.eclipse.jgit"                  %  "org.eclipse.jgit"            % "4.0.1.201506240215-r",
      "org.scalaj" %% "scalaj-http" % "2.3.0",
      "org.mockito" % "mockito-core" % "1.10.19" % "test",
      "org.scalatest" %% "scalatest" % "3.0.4" % "test"
    )
  )
