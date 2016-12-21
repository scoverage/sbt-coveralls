package org.scoverage.coveralls

import _root_.sbt.Keys._
import _root_.sbt.ScopeFilter
import _root_.sbt.ThisProject
import _root_.sbt._
import com.fasterxml.jackson.core.JsonEncoding
import sbt.Keys._
import sbt._
import scala.io.{ Codec, Source }
import java.io.File

object Imports {
  object CoverallsKeys {
    val coverallsFile = SettingKey[File]("coveralls-file")
    val projectBaseDir = SettingKey[File]("base-dir")
    val coverallsToken = SettingKey[Option[String]]("coveralls-repo-token")
    val coverallsTokenFile = SettingKey[Option[String]]("coveralls-token-file")
    val coverallsServiceName = SettingKey[Option[String]]("coverallsServiceName")
    val coverallsFailBuildOnError = SettingKey[Boolean]("fail build if coveralls step fails")
    val coberturaFile = SettingKey[File]("coberturaFile")
    val coverallsEncoding = SettingKey[String]("encoding")
    val coverallsSourceRoots = SettingKey[Seq[Seq[File]]]("Source roots")
    val coverallsEndpoint = SettingKey[Option[String]]("coveralls-endpoint")
    val gitRepoDir = SettingKey[String]("git-repo-dir")
  }
}

object CoverallsPlugin extends AutoPlugin with CommandSupport {
  override def trigger = allRequirements

  val autoImport = Imports
  import autoImport._
  import CoverallsKeys._

  lazy val coverallsCommand = Command.command("coveralls")(doCoveralls)

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    commands += coverallsCommand,
    coverallsFailBuildOnError := false,
    coverallsEncoding := "UTF-8",
    coverallsToken := None,
    coverallsTokenFile := None,
    coverallsEndpoint := Option("https://coveralls.io"),
    projectBaseDir := baseDirectory.value,
    coverallsServiceName := travisJobIdent map { _ => "travis-ci" },
    coverallsFile := crossTarget.value / "coveralls.json",
    coberturaFile := crossTarget.value / ("coverage-report" + File.separator + "cobertura.xml"),
    coverallsSourceRoots := sourceDirectories.all(aggregateFilter).value,
    gitRepoDir := "."
  )

  val aggregateFilter = ScopeFilter(inAggregates(ThisProject), inConfigurations(Compile)) // must be outside of the 'coverageAggregate' task (see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780)

  def doCoveralls(state: State): State = {
    implicit val iState = state
    val extracted = Project.extract(state)
    implicit val pr = extracted.currentRef
    implicit val bs = extracted.structure

    val repoToken = userRepoToken(coverallsToken.gimme, coverallsTokenFile.gimme)

    if (travisJobIdent.isEmpty && repoToken.isEmpty) {
      log.error("Could not find coveralls repo token or determine travis job id")
      log.error(" - If running from travis, make sure the TRAVIS_JOB_ID env variable is set")
      log.error(
        " - Otherwise, to set up your repo token read https://github.com/scoverage/sbt-coveralls#specifying-your-repo-token"
      )
      state.fail
    }

    //Users can encode their source files in whatever encoding they desire, however when we send their source code to
    //the coveralls API, it is a JSON payload. RFC4627 states that JSON must be UTF encoded.
    //See http://tools.ietf.org/html/rfc4627#section-3
    val sourcesEnc = Codec(coverallsEncoding.gimme)
    val jsonEnc = JsonEncoding.UTF8

    val endpoint = userEndpoint(coverallsEndpoint.gimme).get

    val coverallsClient = new CoverallsClient(endpoint, apiHttpClient, sourcesEnc, jsonEnc)

    val writer = new CoverallPayloadWriter(
      projectBaseDir.gimme,
      coverallsFile.gimme,
      repoToken,
      travisJobIdent,
      coverallsServiceName.gimme,
      new GitClient(gitRepoDir.gimme)(log),
      sourcesEnc,
      jsonEnc
    )

    writer.start(log)

    val report = CoberturaFile(coberturaFile.gimme, baseDirectory.gimme)
    if (!report.exists) {
      log.error("Could not find the cobertura.xml file. Did you call coverageAggregate?")
      state.fail
    }

    // include all of the sources (stanard roots and multi-module roots)
    val sources: Seq[File] = (sourceDirectories in Compile).gimme
    val multiSources: Seq[File] = coverallsSourceRoots.gimme.flatten
    val allSources = sources ++ multiSources

    val reader = new CoberturaMultiSourceReader(report.file, allSources, sourcesEnc)
    val sourceFiles = reader.sourceFilenames

    sourceFiles.foreach(sourceFile => {
      val sourceReport = reader.reportForSource(sourceFile)
      writer.addSourceFile(sourceReport)
    })

    writer.end()

    val res = coverallsClient.postFile(coverallsFile.gimme)
    if (res.error) {
      log.error(s"Uploading to $endpoint failed: " + res.message)
      if (res.message.contains(CoverallsClient.tokenErrorString)) {
        log.error(
          "The error message '" + CoverallsClient.tokenErrorString +
            "' can mean your repo token is incorrect."
        )
      } else {
        log.error(s"$endpoint server internal error: " + res.message)
      }
      if (coverallsFailBuildOnError.gimme)
        state.fail
      else
        state
    } else {
      log.info(s"Uploading to $endpoint succeeded: " + res.message)
      log.info(res.url)
      log.info("(results may not appear immediately)")
      state
    }
  }

  def apiHttpClient = new ScalaJHttpClient

  def travisJobIdent = sys.env.get("TRAVIS_JOB_ID")

  def repoTokenFromFile(path: String) = {
    try {
      val source = Source.fromFile(path)
      val repoToken = source.mkString.trim
      source.close()
      Option(repoToken)
    } catch {
      case e: Exception => None
    }
  }

  def userRepoToken(coverallsToken: Option[String], coverallsTokenFile: Option[String]) =
    sys.env.get("COVERALLS_REPO_TOKEN")
      .orElse(coverallsToken)
      .orElse(coverallsTokenFile.flatMap(repoTokenFromFile))

  def userEndpoint(coverallsEndpoint: Option[String]) =
    sys.env.get("COVERALLS_ENDPOINT")
      .orElse(coverallsEndpoint)
}

case class CoberturaFile(file: File, projectBase: File) {
  def exists = file.exists
}
