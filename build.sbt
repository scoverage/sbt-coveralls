name := "xsbt-coveralls-plugin"

organization  := "com.github.theon"

version       := "0.0.1-SNAPSHOT"

scalaVersion  := "2.9.2"

sbtPlugin := true

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= Seq (
  "org.codehaus.jackson" % "jackson-core-asl" % "1.9.3",
  "com.fasterxml" % "jackson-module-scala" % "1.9.3",
  "org.scalaj" %% "scalaj-http" % "0.3.6"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.9.1" % "test"
)

seq(ScctPlugin.instrumentSettings : _*)

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/theon/scala-uri</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:theon/xsbt-coveralls-plugin.git</url>
    <connection>scm:git@github.com:theon/xsbt-coveralls-plugin.git</connection>
  </scm>
  <developers>
    <developer>
      <id>theon</id>
      <name>Ian Forsey</name>
      <url>http://theon.github.com</url>
    </developer>
  </developers>)
