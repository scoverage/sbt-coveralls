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

  def userRepoToken(coverallsToken: Option[String], coverallsTokenFile: Option[String]) =
    sys.env.get("COVERALLS_REPO_TOKEN")
      .orElse(coverallsToken)
      .orElse(coverallsTokenFile.flatMap(repoTokenFromFile))
      .orElse(userRepoTokenFromLegacyFile)

  object CoverallsKeys {
    //So people can configure coverallsToken := "string" even though it is a SettingKey[Option[String]]
    //Better way to do this?
    implicit def strToOpt(s: String) = Option(s)

    val coverallsTask = TaskKey[Unit]("coveralls", "Generate coveralls reports")
    val encoding = SettingKey[String]("encoding")
    val coverallsToken = SettingKey[Option[String]]("coveralls-repo-token")
    val coverallsTokenFile = SettingKey[Option[String]]("coveralls-token-file")
  }

  override lazy val settings = Seq (
    encoding := "UTF-8",
    coverallsToken := None,
    coverallsTokenFile := None,
    coverallsTask <<= (state, baseDirectory, crossTarget, encoding, coverallsToken, coverallsTokenFile) map {
      (state, baseDirectory, crossTarget, encoding, coverallsToken, coverallsTokenFile) => {
        val coberturaFile = crossTarget.getAbsolutePath + "/coverage-report/cobertura.xml"
        val coverallsFile = crossTarget.getAbsolutePath + "/coveralls.json"
        val projectDirectory = baseDirectory.getAbsoluteFile + "/"
        coverallsCommand(state, projectDirectory, coberturaFile, coverallsFile, encoding, coverallsToken, coverallsTokenFile)
      }
    }
  )
}

trait AbstractCoverallsPlugin  {

  def apiHttpClient:HttpClient

  def coverallsCommand(state: State,
                        projectDirectory: String,
                        coberturaFile: String,
                        coverallsFile: String,
                        encoding: String,
                        coverallsToken: Option[String],
                        coverallsTokenFile: Option[String]) = {

    val repoToken = userRepoToken(coverallsToken, coverallsTokenFile)

    if(travisJobIdent.isEmpty && repoToken.isEmpty) {
      state.log.error("Could not find coveralls repo token or determine travis job id")
      state.log.error(" - If running from travis, make sure the TRAVIS_JOB_ID env variable is set")
      state.log.error(" - Otherwise, to set up your repo token read https://github.com/theon/xsbt-coveralls-plugin#specifying-your-repo-token")
      state.fail
    } else {

      //Run the scct plugin to generate code coverage
      Command.process("scct:test", state)

      val reader = new CoberturaReader(coberturaFile)

      val writer = new CoverallPayloadWriter (
        coverallsFile,
        repoToken,
        travisJobIdent,
        new GitClient
      )

      val coverallsClient = new CoverallsClient(apiHttpClient)
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

  def travisJobIdent: Option[String]
  def userRepoToken(coverallsToken: Option[String], coverallsTokenFile: Option[String]): Option[String]

  def repoTokenFromFile(path: String) = {
    try {
      val source = Source.fromFile(path)
      val repoToken = source.mkString.trim
      source.close
      Option(repoToken)
    } catch {
      case e: Exception => None
    }
  }

  @deprecated("Put your repo token in ~/.sbt/coveralls.sbt instead. Use this format: coverallsRepoToken := \"my-token\"", "June 2013")
  def userRepoTokenFromLegacyFile =
    repoTokenFromFile(Path.userHome.getAbsolutePath + "/.sbt/coveralls.repo.token")
}