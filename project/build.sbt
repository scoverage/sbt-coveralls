resolvers += Classpaths.typesafeResolver

resolvers ++= Seq(
  "scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo",
  "sonatype-oss-repo" at "https://oss.sonatype.org/content/groups/public/"
)

offline := true

addSbtPlugin("reaktor" %% "sbt-scct" % "0.2-SNAPSHOT")