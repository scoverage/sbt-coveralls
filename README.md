# sbt-coveralls

SBT plugin that uploads scala code coverage to [https://coveralls.io](https://coveralls.io) and integrates with [Travis CI](#travis-ci-integration).
This plugin uses either [scoverage](http://mtkopone.github.com/scoverage/) or [SCCT](http://mtkopone.github.io/scct/) to generate the code coverage metrics.
This repo is a fork of the original [xsbt-coverall-plugin](https://github.com/theon/xsbt-coveralls-plugin) to add
support for scoverage. Use this project if you need scoverage support and until/if scoverage is supported upstream in
 xsbt-coveralls-plugin.

For an example project that uses this plugin [click here](https://github.com/scoverage/scoverage-samples).
For example output [click here](https://coveralls.io/r/scoverage/scoverage-samples)

## Installation

1) Add the following to your `project/build.sbt` file

```scala
resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.sksamuel.scoverage" % "sbt-scoverage" % "0.95.1")

addSbtPlugin("com.sksamuel.scoverage" %% "sbt-coveralls" % "0.0.5")
```

2) Add the following to the top of your `build.sbt`

```scala
ScoverageSbtPlugin.instrumentSettings

CoverallsPlugin.coverallsSettings
```

Coveralls configuration options (such as [Specifying Your Repo Token](#specifying-your-repo-token)) must come after this line.

3) Register on `https://coveralls.io/`

4) Follow the instructions for either [Travis CI](#travis-ci-integration) or [Manual Usage](#manual-usage)

## Travis CI Integration

`sbt-coveralls` can be run by [Travis CI](http://about.travis-ci.org/) by following these instructions:

1) Add the following to you `travis.yml`

    script: "sbt coveralls"

2) Job done! Commit these changes to `travis.yml` to kick off your Travis build and you should see coverage reports appear on http://coveralls.io

## Manual Usage

1)  Get the repo token for your repo from http://coveralls.io

1) Let `sbt-coveralls` know what your coveralls repo token is. See [Specifying Your Repo Token](#specifying-your-repo-token)

2) In the SBT console run the command `coveralls`. This should run your test suite, generate code coverage reports and upload the reports to `coveralls.io`. After running the command, you should see output similar to the following:

    Uploading to coveralls.io succeeded: Job #17.1
    https://coveralls.io/jobs/12207

For example output [click here](https://coveralls.io/builds/6727)

## Specifying Your Repo Token

There are several ways to tell `sbt-coveralls` your repo token to support different use cases:

### Write your repo token into a file 

Add the following to your `build.sbt`. The path can be absolute and point to somewhere outside the project or relative and point somewhere inside the project (such as `src/main/resources/token.txt`). 

Just remember: **Do not store repo tokens inside your project if it is in a public git repository!**

```scala
import com.github.theon.coveralls.CoverallsPlugin.CoverallsKeys._

coverallsTokenFile := "/path/to/my/repo/token.txt"
```

### Put your repo token directly in your `build.sbt`

**Do not store repo tokens inside your project if it is in a public git repository!**

```scala
import com.github.theon.coveralls.CoverallsPlugin.CoverallsKeys._

coverallsToken := "my-token"
```

### Add an environment variable

Add an environment variable `COVERALLS_REPO_TOKEN`, for example:

    export COVERALLS_REPO_TOKEN=my-token

## Custom Source File Encoding

By default `sbt-coveralls` assumes your source files are `UTF-8` encoded. To use a different encoding, add the following to your `build.sbt`

```scala
import CoverallsPlugin.CoverallsKeys._

encoding := "ISO-8859-1"
```

Once the plugin has slurped your source code into memory using the specified encoding, it will be converted into UTF-8 to be sent to the coveralls API. This is because the coveralls API uses a JSON request body and RFC 4627 mandates that [JSON must be UTF encoded](http://tools.ietf.org/html/rfc4627#section-3).

# License

`sbt-coveralls` is open source software released under the [Apache 2 License](http://www.apache.org/licenses/LICENSE-2.0).
