name := "sbt-coveralls"

organization := "org.scoverage"

scalaVersion := "2.10.5"

sbtPlugin := true

publishMavenStyle := false

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

dependencyOverrides ++= Set(
  "com.jcraft"                        %  "jsch"                        % "0.1.51"
)

libraryDependencies ++= Seq (
  "com.fasterxml.jackson.core"        %  "jackson-core"                % "2.5.1",
  "com.fasterxml.jackson.module"      %% "jackson-module-scala"        % "2.5.1",
  "org.eclipse.jgit"                  %  "org.eclipse.jgit"            % "3.7.0.201502260915-r",
// major version change - needs more investigation/testing
//  "org.scalaj"                        %% "scalaj-http"                 % "1.1.4",
  "org.scalaj"                        %% "scalaj-http"                 % "0.3.16",
  "org.mockito"                       %  "mockito-core"                % "1.9.5"         % "test",
  "org.scalatest"                     %% "scalatest"                   % "2.2.1"         % "test"
)

scalariformSettings

publishMavenStyle := false

publishArtifact in Test := false

publishTo := {
  if (isSnapshot.value)
    Some(Classpaths.sbtPluginSnapshots)
  else
    Some(Classpaths.sbtPluginReleases)
}

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
