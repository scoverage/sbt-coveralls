import bintray.Keys._

name := "sbt-coveralls"

organization := "com.sksamuel.scoverage"

version       := "0.0.5"

scalaVersion := "2.10.3"

sbtPlugin := true

publishMavenStyle := false

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

bintrayPublishSettings

repository in bintray := "sbt-plugins"

licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization in bintray := None

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq (
  "com.fasterxml.jackson.core" % "jackson-core" % "2.2.3",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.2.3",
  "org.scalaj" %% "scalaj-http" % "0.3.6",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "org.mockito" % "mockito-core" % "1.9.5"
)

pomExtra := <url>https://github.com/scoverage/sbt-coveralls</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:scoverage/sbt-coveralls.git</url>
    <connection>scm:git@github.com:scoverage/sbt-coveralls.git</connection>
  </scm>
  <developers>
    <developer>
      <id>theon</id>
      <name>Ian Forsey</name>
      <url>http://theon.github.com</url>
    </developer>
    <developer>
      <id>sksamuel</id>
      <name>Stephen Samuel</name>
      <url>http://github.com/sksamuel</url>
    </developer>
  </developers>
