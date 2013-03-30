# xsbt-coverall-plugin

[![Build Status](https://travis-ci.org/theon/xsbt-coveralls-plugin.png?branch=master)](https://travis-ci.org/theon/xsbt-coveralls-plugin)
[![Coverage Status](https://coveralls.io/repos/theon/xsbt-coveralls-plugin/badge.png?branch=master)](https://coveralls.io/r/theon/xsbt-coveralls-plugin)

SBT plugin that uploads scala code coverage to [https://coveralls.io](https://coveralls.io) and integrates with [Travis CI](#travis-ci-integration). This plugin uses [scct](http://mtkopone.github.com/scct/) to generate the code coverage metrics.

For an example project that uses this plugin [click here](https://github.com/theon/scala-uri)

For example output [click here](https://coveralls.io/builds/6727)

# Installation

1) Adding the following to your `project/build.sbt` file

```scala
resolvers += Classpaths.typesafeResolver

resolvers ++= Seq(
    "scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo"
)

addSbtPlugin("reaktor" % "sbt-scct" % "0.2-SNAPSHOT")

addSbtPlugin("com.github.theon" %% "xsbt-coveralls-plugin" % "0.0.1")
```

2) Add the following to your `build.sbt`

```scala
seq(ScctPlugin.instrumentSettings : _*)
```

3) Register on `https://coveralls.io/` and get a repo token

4) Follow the instructions for either [Travis CI](#travis-ci-integration) or [Manual Usage](#manual-usage)

# Travis CI Integration

`xsbt-coverall-plugin` can be run by [Travis CI](http://about.travis-ci.org/) by following these instructions:

1) Add the following to you `travis.yml`

    script: "sbt coveralls test"

2) Job done! Commit these changes to `travis.yml` to kick off your Travis build and you should see coverage reports appear on http://coveralls.io

# Manual Usage

1) Either write your token into the file `~/.sbt/coveralls.repo.token` or into the environment variable `COVERALLS_REPO_TOKEN`

    export COVERALLS_REPO_TOKEN=<your-coveralls-repo-token>

2) In the SBT console run the command `coveralls test`. This should run your test suite, generate code coverage reports and upload the reports to `coveralls.io`. After running the command, you should see output similar to the following:

    Uploading to coveralls.io succeeded: Job #17.1
    https://coveralls.io/jobs/12207

For example output [click here](https://coveralls.io/builds/6727)

# SNAPSHOT Builds

Add the following to your `project/build.sbt` file

```scala
resolvers += Classpaths.typesafeResolver

resolvers ++= Seq(
    "scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo",
    "sonatype-oss-repo" at "https://oss.sonatype.org/content/groups/public/"
)

addSbtPlugin("reaktor" % "sbt-scct" % "0.2-SNAPSHOT")

addSbtPlugin("com.github.theon" %% "xsbt-coveralls-plugin" % "0.0.2-SNAPSHOT")
```

# TODO

For a list of features that going to be implemented see the [issue tracker](https://github.com/theon/xsbt-coveralls-plugin/issues?labels=enhancement&page=1&state=open)

# License

`xsbt-coveralls-plugin` is open source software released under the [Apache 2 License](http://www.apache.org/licenses/LICENSE-2.0).