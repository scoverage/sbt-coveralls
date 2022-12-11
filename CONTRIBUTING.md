# Contributing

## Maintainers/Contributers

* Create an issue (and agree what needs to get solved and how)
* Fork the repo
* Run `sbt generateXMLFiles` (once)
* Run `sbt test`to test the current implementation
* Run `sbt publishLocal` to create a local build that you can use in your projects `plugins.sbt` file to test something locally
* Create a PR (and work through the feedback on the PR)

## Committers

* Merge the PR(s)
* When ready, tag the current commit with `git tag v<next-version>`
* Push the tag with `git push --tags`
* (For now) Go into `releases` and promote the draft release to latest

Note: (Obviously) Be careful to push tags, because this will trigger
a push to sonatype/maven and bad release/tags cannot be overwritten or
deleted.
