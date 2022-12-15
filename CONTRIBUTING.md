# Contributing

## Maintainers/Contributers

* Create an issue (and agree what needs to get solved and how)
* Fork the repo
* Run `sbt generateXMLFiles` (once)
* Run `sbt test`to test the current implementation
* Run `sbt publishLocal` to create a local build that you can use in
  your projects `plugins.sbt` file to test something locally
* Run `sbt scripted` to test the plugin with the scripted test-cases
* Create a PR (and work through the feedback on the PR)

## Contributing an sbt-test

* Reference [sbt-coveralls-test][] as an example
* Add a/the `test` file to the root of your repo
* Put the coveralls token into a `.coverallsToken` file in the root of
  your repo and reference/use it from the build.sbt file (not a best
  practice, but good enough for now)
* Update your `plugins.sbt` file to read the plugin versions from
  the system properties
* Run `git submodule add <repo-url> src/sbt-test/scoverage/<repo-name>`
* Run `sbt prepareScripted scripted`
* To update/pull the latest tests run `git submodule update --remote`

## Committers

* Merge the PR(s)
* When ready, tag the current commit with `git tag v<next-version>`
* Push the tag with `git push --tags`
* (For now) Go into `releases` and promote the draft release to latest

Note: (Obviously) Be careful to push tags, because this will trigger
a push to sonatype/maven and bad release/tags cannot be overwritten or
deleted.

[sbt-coveralls-test]: https://github.com/rolandtritsch/sbt-coveralls-test
