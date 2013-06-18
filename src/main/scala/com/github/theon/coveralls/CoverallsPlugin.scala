package com.github.theon.coveralls

import sbt.Keys._
import sbt._
import scala.io.{Codec, Source}
import tools.nsc.Global
import annotation.tailrec
import scala.Some
import scala.Some
import com.github.theon.coveralls.CoverallsPlugin.CoverallsKeys

/**
 * Date: 10/03/2013
 * Time: 17:01
 */
object CoverallsPlugin extends Plugin with AbstractCoverallsPlugin {

  import CoverallsKeys._

  def apiHttpClient = new ScalaJHttpClient

  def travisJobIdent = sys.env.get("TRAVIS_JOB_ID")
  def userRepoToken = sys.env.get("COVERALLS_REPO_TOKEN").orElse(userRepoTokenFromFile)

  object CoverallsKeys {
    val coverallsTask = TaskKey[Unit]("coveralls", "Generate coveralls reports")
    val encoding = SettingKey[String]("encoding")
  }

  override lazy val settings = Seq (
    encoding := "UTF-8",
    coverallsTask <<= (state, baseDirectory, crossTarget, encoding) map {
      (state, baseDirectory, crossTarget, encoding) => {
        val coberturaFile = crossTarget.getAbsolutePath + "/coverage-report/cobertura.xml"
        val coverallsFile = crossTarget.getAbsolutePath + "/coveralls.json"
        val projectDirectory = baseDirectory.getAbsoluteFile + "/"
        coverallsCommand(state, projectDirectory, coberturaFile, coverallsFile, encoding)
      }
    }
  )
}

trait AbstractCoverallsPlugin  {

  def apiHttpClient:HttpClient

  def coverallsCommand(state: State, projectDirectory: String, coberturaFile: String, coverallsFile: String, encoding: String) = {

    if(travisJobIdent.isEmpty && userRepoToken.isEmpty) {
      state.log.error("Could not find coveralls repo token or determine travis job id")
      state.log.error(" - If running from travis, make sure the TRAVIS_JOB_ID env variable is set")
      state.log.error(" - Otherwise, make sure the COVERALLS_REPO_TOKEN env variable is set or that the repo token is written inside " + userRepoTokenFilePath)
      state.fail
    } else {

      //Run the scct plugin to generate code coverage
      Command.process("scct:test", state)

      val reader = new CoberturaReader {
        def file = coberturaFile
      }

      val writer = new CoverallPayloadWriter {
        def repoToken = userRepoToken
        def file = coverallsFile
        def travisJobId = travisJobIdent
        val gitClient = new GitClient {}
      }

      val coverallsClient = new CoverallsClient {
        def httpClient = apiHttpClient
      }
      val sourceFiles = reader.sourceFilenames()
      writer.start(state.log)

      sourceFiles.foreach(sourceFile => {
        val sourceReport = reader.reportForSource(projectDirectory, sourceFile)
        writer.addSourceFile(sourceReport)
      })

      writer.end()

      val res = coverallsClient.postFile(coverallsFile, Codec(encoding))
      if(res.error) {
        state.log.error("Uploading to coveralls.io failed: " + res.message)
        if(res.message.contains("Build processing error")) {
          state.log.error("The error message 'Build processing error' can mean your repo token is incorrect. See https://github.com/lemurheavy/coveralls-public/issues/46")
        }
        state.fail
      } else {
        state.log.info("Uploading to coveralls.io succeeded: " + res.message)
        state.log.info(res.url)
        state.log.info("(results may not appear immediately)")
      }
    }
  }

  def travisJobIdent:Option[String]
  def userRepoToken:Option[String]

  def userRepoTokenFilePath = Path.userHome.getAbsolutePath + "/.sbt/coveralls.repo.token"
  def userRepoTokenFromFile = {
    try {
      Some(Source.fromFile(userRepoTokenFilePath).mkString.trim)
    } catch {
      case e:Exception => None
    }
  }
}