import ReleaseTransformations._

name := "sbt-coveralls"

organization := "org.scoverage"

sbtPlugin := true

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

dependencyOverrides ++= Set(
  "com.jcraft"                        %  "jsch"                        % "0.1.51"
)

crossSbtVersions := Vector("0.13.16", "1.0.0")

libraryDependencies ++= Seq (
  "com.fasterxml.jackson.core"        %  "jackson-core"                % "2.9.0",
  "com.fasterxml.jackson.module"      %% "jackson-module-scala"        % "2.9.0",
  // DO NOT UPGRADE: later versions of jgit use Java 7 and we still need to support Java 6
  "org.eclipse.jgit"                  %  "org.eclipse.jgit"            % "3.7.0.201502260915-r",
  //"org.eclipse.jgit"                  %  "org.eclipse.jgit"            % "4.0.1.201506240215-r",
  "org.scalaj"                        %% "scalaj-http"                 % "2.3.0",
  "org.mockito"                       %  "mockito-core"                % "1.10.19"       % "test",
  "org.scalatest"                     %% "scalatest"                   % "3.0.4"         % "test"
)

scalariformSettings

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
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

// We redefine the release process so that we use SBT plugin cross building operator (^)
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepCommandAndRemaining("^test"),
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("^publishSigned"),
  setNextVersion,
  commitNextVersion,
  pushChanges)
