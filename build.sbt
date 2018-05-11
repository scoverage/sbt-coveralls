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

crossSbtVersions := Seq("0.13.17", "1.1.4")

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
