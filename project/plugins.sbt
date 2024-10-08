libraryDependencies += "org.scala-sbt" % "scripted-plugin_2.12" % sbtVersion.value

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.7.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
