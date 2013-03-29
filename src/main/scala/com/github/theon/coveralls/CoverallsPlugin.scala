package com.github.theon.coveralls

import sbt.Keys._
import sbt.{Path, SettingKey, Command, Plugin}
import io.Source

/**
 * Date: 10/03/2013
 * Time: 17:01
 */
object CoverallsPlugin extends Plugin {

  override lazy val settings = Seq(commands += coverallCommand)

  val coberturaFile = "target/scala-2.10/coverage-report/cobertura.xml"
  val coverallsFile = "target/scala-2.10/coverage-report/coveralls.json"

  def coverallCommand = Command.args("coveralls", "test") { (state, args) =>
    //Run the scct plugin to generate code coverage
    Command.process("scct:test", state)

    val baseDir = state.configuration.baseDirectory.getAbsolutePath

    val reader = new CoberturaReader {
      def file = baseDir + "/" + coberturaFile
    }

    val writer = new CoverallPayloadWriter {
      def repoToken = userRepoToken
      def file = baseDir + "/" + coverallsFile
      def travisJobId = travisJobIdent
    }

    val coverallsClient = new CoverallsClient {}
    val sourceFiles = reader.sourceFilenames()
    writer.start(state)

    sourceFiles.foreach(sourceFile => {
      val sourceReport = reader.reportForSource(baseDir, sourceFile)
      writer.addSouceFile(sourceReport)
    })

    writer.end()

    val res = coverallsClient.postFile(coverallsFile)
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