# sbt-coveralls

[![License](http://img.shields.io/:license-Apache%202-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Join the chat at https://gitter.im/scoverage/sbt-coveralls](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scoverage/sbt-coveralls?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.scoverage/sbt-coveralls/badge.svg?kill_cache=1)](https://search.maven.org/artifact/org.scoverage/sbt-coveralls/)

SBT plugin that uploads scala code coverage to [https://coveralls.io](https://coveralls.io) and integrates with [Travis CI](#travis-ci-integration) and [GitHub Actions](#github-actions-integration). This plugin uses [scoverage](https://github.com/scoverage/scalac-scoverage-plugin/) to generate the code coverage metrics.

For an example project that uses this plugin [click here](https://github.com/scoverage/sbt-scoverage-samples).
For example output [click here](https://coveralls.io/r/scoverage/scoverage-samples)

## Installation

1) Add the following to your `project/build.sbt` file

```scala

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.8.2")

addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.1")
```

2) Setup coveralls configuration options (such as [Specifying Your Repo Token](#specifying-your-repo-token))

3) Register on `https://coveralls.io/`

4) Follow the instructions for either [Travis CI](#travis-ci-integration) or [Manual Usage](#manual-usage)

## Travis CI Integration

`sbt-coveralls` can be run by [Travis CI](https://docs.travis-ci.com/) by following these instructions:

1) Add the following to your `travis.yml`

    ```yaml
    script: "sbt clean coverage test"
    after_success: "sbt coverageReport coveralls"
    ```

   If you have a multi-module project, perform `coverageAggregate`
   [as a separate command](https://github.com/scoverage/sbt-scoverage#multi-project-reports)

    ```yaml
    script:
      - sbt clean coverage test coverageReport &&
        sbt coverageAggregate
    after_success:
      - sbt coveralls
    ```

2) Job done! Commit these changes to `travis.yml` to kick off your Travis build and you should see coverage reports appear on https://coveralls.io/

## GitHub Actions Integration

`sbt-coveralls` can be run by [GitHub Actions](https://github.com/features/actions) by following these instructions:

1) Add the following to your `.github/workflows/ci.yml`

    ```yaml
    - name: Git checkout (merge)
      uses: actions/checkout@v3
      if: github.event_name != 'pull_request'
      with:
        fetch-depth: 0

    - name: Git checkout (PR)
      uses: actions/checkout@v3
      if: github.event_name == 'pull_request'
      with:
        fetch-depth: 0
        # see: https://frontside.com/blog/2020-05-26-github-actions-pull_request/#how-does-pull_request-affect-actionscheckout
        ref: ${{ github.event.pull_request.head.sha }}

    - name: Run tests
      run: sbt clean coverage test

    - name: Upload coverage data to Coveralls
      run: sbt coverageReport coveralls
      env:
        COVERALLS_REPO_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        COVERALLS_FLAG_NAME: Scala ${{ matrix.scala }}
    ```

    Note the separate checkout step for pull requests.
    It is needed because of
    [the way pull_request affects actions checkout](https://frontside.com/blog/2020-05-26-github-actions-pull_request/#how-does-pull_request-affect-actionscheckout),
    so correct commit info could be sent to coveralls.io

    If you have a multi-module project, perform `coverageAggregate`
    [as a separate command](https://github.com/scoverage/sbt-scoverage#multi-project-reports)

    ```yaml
    - name: Upload coverage data to Coveralls
      run: sbt coverageAggregate coveralls
      env:
        COVERALLS_REPO_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        COVERALLS_FLAG_NAME: Scala ${{ matrix.scala }}
    ```

2) Job done! Commit these changes to kick off your GitHub Actions build and you should see coverage reports appear on https://coveralls.io/

## Manual Usage

1)  Get the repo token for your repo from http://coveralls.io

1) Let `sbt-coveralls` know what your coveralls repo token is. See [Specifying Your Repo Token](#specifying-your-repo-token)

2) In the SBT console, run `coverage` then your tests finishing with `coveralls`. After running the command, you should see output similar to the following:

       Uploading to coveralls.io succeeded: Job #17.1
       https://coveralls.io/jobs/12207

For example output [click here](https://coveralls.io/builds/6727)

## Specifying Your Repo Token

There are several ways to tell `sbt-coveralls` your repo token to support different use cases:

### Write your repo token into a file

Add the following to your `build.sbt`. The path can be absolute and point to somewhere outside the project or relative and point somewhere inside the project (such as `src/main/resources/token.txt`).

Just remember: **Do not store repo tokens inside your project if it is in a public git repository!**

```scala
import org.scoverage.coveralls.Imports.CoverallsKeys._

coverallsTokenFile := "/path/to/my/repo/token.txt"
```

### Put your repo token directly in your `build.sbt`

**Do not store repo tokens inside your project if it is in a public git repository!**

```scala
import org.scoverage.coveralls.Imports.CoverallsKeys._

coverallsToken := Some("my-token")
```

### Add an environment variable

Add an environment variable `COVERALLS_REPO_TOKEN`, for example:

    export COVERALLS_REPO_TOKEN=my-token

## Specifying Your Coveralls Endpoint

If you're using https://coveralls.io as your endpoint, then you don't need to set this option. If you're using a hosted (enterprise) instance of coveralls, you will need to specify your endpoint in one of two ways.

### Put your endpoint directly in your `build.sbt`

```scala
import org.scoverage.coveralls.Imports.CoverallsKeys._

coverallsEndpoint := Some("http://my-instance.com")
```

### Add an environment variable

Add an environment variable `COVERALLS_ENDPOINT`, for example:

    export COVERALLS_ENDPOINT=http://my-instance.com

## Overriding the current branch

By default `sbt-coveralls` uses the currently checked-out branch for reporting. To override the branch name add the `CI_BRANCH` variable, for example:

    export CI_BRANCH=my-branch-name

## Specifying Source File Encoding

`sbt-coveralls` finds the encoding in `scalacOptions` setting value.
If not defined it assumes source files are encoded using platform-specific encoding.
To specify encoding, add the following to your `build.sbt`

```scala
scalacOptions += Seq("-encoding", "UTF-8")
```

## Using Travis-Pro

It is important to set the correct `service` when using Travis-Pro.  The default is to use `travis-ci`.  To override this value, add the following to your `build.sbt`

```scala
import org.scoverage.coveralls.Imports.CoverallsKeys._
import org.scoverage.coveralls.TravisPro

coverallsService := Some(TravisPro)
```

# License

`sbt-coveralls` is open source software released under the [Apache 2 License](http://www.apache.org/licenses/LICENSE-2.0).
