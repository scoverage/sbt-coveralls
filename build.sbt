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
    scalaVersion := "2.13.6"
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
      "com.fasterxml.jackson.core" % "jackson-core" % "2.12.3",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.3",
      "org.eclipse.jgit" % "org.eclipse.jgit" % "5.11.1.202105131744-r",
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "org.mockito" % "mockito-core" % "3.10.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.9" % Test
    )
  )
