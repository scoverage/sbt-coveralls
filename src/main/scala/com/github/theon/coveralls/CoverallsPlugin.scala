package com.github.theon.coveralls

import sbt.Keys._
import sbt._
import io.Source

/**
 * Date: 10/03/2013
 * Time: 17:01
 */
object CoverallsPlugin extends AbstractCoverallsPlugin {
  def coberturaFile(state:State) = baseDir(state) + "target/scala-2.10/coverage-report/cobertura.xml"
  def coverallsFile(state:State) = baseDir(state) + "target/scala-2.10/coverage-report/coveralls.json"
  def apiHttpClient = new ScalaJHttpClient
  def baseDir(state:State) = state.configuration.baseDirectory.getAbsolutePath + "/"
}
trait AbstractCoverallsPlugin extends Plugin {

  override lazy val settings = Seq(commands += Command.args("coveralls", "test")(coverallsCommand))

  def coberturaFile(state:State):String
  def coverallsFile(state:State):String
  def baseDir(state:State):String

  def apiHttpClient:HttpClient

  def coverallsCommand = (state:State, args:Seq[String]) => {
    //Run the scct plugin to generate code coverage
      Command.process("scct:test", state)

    val reader = new CoberturaReader {
      def file = coberturaFile(state)
    }

    val writer = new CoverallPayloadWriter {
      def repoToken = userRepoToken
      def file = coverallsFile(state)
      def travisJobId = travisJobIdent
      val gitClient = new GitClient {}
    }

    val coverallsClient = new CoverallsClient {
      def httpClient = apiHttpClient
    }
    val sourceFiles = reader.sourceFilenames()
    writer.start(state.log)

    sourceFiles.foreach(sourceFile => {
      val sourceReport = reader.reportForSource(baseDir(state), sourceFile)
      writer.addSourceFile(sourceReport)
    })

    writer.end()

    val res = coverallsClient.postFile(coverallsFile(state))
    if(res.error) {
      state.log.error("Uploading to coveralls.io failed: " + res.message)
      state.fail
    } else {
      state.log.info("Uploading to coveralls.io succeeded: " + res.message)
      state.log.info(res.url)
      state
    }
  }

  def travisJobIdent = sys.env.get("TRAVIS_JOB_ID")

  def userRepoToken =
    sys.env.getOrElse("COVERALLS_REPO_TOKEN", {
      Source.fromFile(Path.userHome.getAbsolutePath + "/.sbt/coveralls.repo.token").mkString
    })
}