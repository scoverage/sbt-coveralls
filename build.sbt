name := "sbt-coveralls"

organization := "org.scoverage"

version       := "1.0.0"

scalaVersion := "2.10.4"

sbtPlugin := true

publishMavenStyle := false

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq (
  "com.fasterxml.jackson.core"        %  "jackson-core"                % "2.4.2",
  "com.fasterxml.jackson.module"      %% "jackson-module-scala"        % "2.4.2",
  "org.eclipse.jgit"                  %  "org.eclipse.jgit"            % "3.4.1.201406201815-r",
  "org.scalaj"                        %% "scalaj-http"                 % "0.3.16",
  "org.mockito"                       %  "mockito-core"                % "1.9.5"         % "test",
  "org.scalatest"                     %% "scalatest"                   % "2.2.1"         % "test"
)

publishMavenStyle := false

publishArtifact in Test := false

publishTo := {
  Some(
    Resolver.url(
      "publishTo",
      new URL("https://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/")
    )(Resolver.ivyStylePatterns)
  )
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
