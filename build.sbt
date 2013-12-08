name := "sbt-coveralls"

organization := "com.sksamuel.scoverage"

version       := "0.0.5"

scalaVersion := "2.10.3"

sbtPlugin := true

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq (
  "com.fasterxml.jackson.core" % "jackson-core" % "2.2.3",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.2.3",
  "org.scalaj" %% "scalaj-http" % "0.3.6"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "org.mockito" % "mockito-core" % "1.9.5"
)

publishTo <<= version {
  (v: String) =>
    val scalasbt = "http://repo.scala-sbt.org/scalasbt/"
    val (name, url) = if (v.trim.endsWith("SNAPSHOT")) ("sbt-plugin-snapshots", scalasbt + "sbt-plugin-snapshots")
    else ("sbt-plugin-releases", scalasbt + "sbt-plugin-releases")
    Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

pomExtra := <url>https://github.com/scoverage/xsbt-coveralls-plugin</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:scoverage/xsbt-coveralls-plugin.git</url>
    <connection>scm:git@github.com:scoverage/xsbt-coveralls-plugin.git</connection>
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
