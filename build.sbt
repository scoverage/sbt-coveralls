name := "xsbt-coveralls-plugin"

organization  := "com.github.theon"

version       := "0.0.5-SNAPSHOT"

scalaVersion  := "2.9.2"

sbtPlugin := true

crossBuildingSettings

CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")

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

libraryDependencies <+= (CrossBuilding.pluginSbtVersion) {
  case v if v startsWith "0.13" => Defaults.sbtPluginExtra("com.github.scct" % "sbt-scct" % "0.3-SNAPSHOT", "0.13", "2.10")
  case v if v startsWith "0.12" => Defaults.sbtPluginExtra("com.github.scct" % "sbt-scct" % "0.3-SNAPSHOT", "0.12", "2.9.2")
}

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "org.mockito" % "mockito-core" % "1.9.5"
)


seq(CoverallsPlugin.singleProject: _*)

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
