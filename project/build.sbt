resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

addSbtPlugin("net.virtual-void" % "sbt-cross-building" % "0.8.0")

addSbtPlugin("com.github.theon" %% "xsbt-coveralls-plugin-meta" % "0.0.5-SNAPSHOT")