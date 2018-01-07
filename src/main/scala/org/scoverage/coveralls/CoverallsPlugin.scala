package org.scoverage.coveralls

import _root_.sbt.ScopeFilter
import _root_.sbt.ThisProject
import com.fasterxml.jackson.core.JsonEncoding
import sbt.Keys._
import sbt._

import scala.io.{Codec, Source}
import java.io.File

import com.sun.xml.internal.ws.api.config.management.policy.ManagementAssertion.Setting

object Imports {
  object CoverallsKeys {
    val coverallsFile = SettingKey[File]("coverallsFile")
    val projectBaseDir = SettingKey[File]("baseDir")
    val coverallsToken = SettingKey[Option[String]]("coverallsRepoToken")
    val coverallsTokenFile = SettingKey[Option[String]]("coverallsTokenFile")
    val coverallsServiceName = SettingKey[Option[String]]("coverallsServiceName")
    val coverallsFailBuildOnError = SettingKey[Boolean](
      "coverallsFailBuildOnError", "fail build if coveralls step fails")
    val coberturaFile = SettingKey[File]("coberturaFile")
    val coverallsEncoding = SettingKey[String]("encoding")
    val coverallsSourceRoots = SettingKey[Seq[Seq[File]]]("coverallsSourceRoots")
    val coverallsEndpoint = SettingKey[Option[String]]("coverallsEndpoint")
    val coverallsGitRepoLocation = SettingKey[Option[String]]("coveralls-git-repo")
  }
}

object CoverallsPlugin extends AutoPlugin {
  override def trigger = allRequirements

  val autoImport = Imports
  import autoImport._
  import CoverallsKeys._

  lazy val coveralls = taskKey[Unit](
    "Uploads scala code coverage to coveralls.io"
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    coveralls := coverallsTask.value,
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
    coverallsGitRepoLocation := Some(".")
  )

  val aggregateFilter = ScopeFilter(inAggregates(ThisProject), inConfigurations(Compile)) // must be outside of the 'coverageAggregate' task (see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780)

  def coverallsTask = Def.task {
    val log = streams.value.log
    val extracted = Project.extract(state.value)
    implicit val pr = extracted.currentRef
    implicit val bs = extracted.structure

    val repoToken = userRepoToken(coverallsToken.value, coverallsTokenFile.value)

    if (travisJobIdent.isEmpty && repoToken.isEmpty) {
      sys.error(
        """
          |Could not find coveralls repo token or determine travis job id
          | - If running from travis, make sure the TRAVIS_JOB_ID env variable is set
          | - Otherwise, to set up your repo token read https://github.com/scoverage/sbt-coveralls#specifying-your-repo-token
        """.stripMargin)
    }

    //Users can encode their source files in whatever encoding they desire, however when we send their source code to
    //the coveralls API, it is a JSON payload. RFC4627 states that JSON must be UTF encoded.
    //See http://tools.ietf.org/html/rfc4627#section-3
    val sourcesEnc = Codec(coverallsEncoding.value)
    val jsonEnc = JsonEncoding.UTF8

    val endpoint = userEndpoint(coverallsEndpoint.value).get

    val coverallsClient = new CoverallsClient(endpoint, apiHttpClient, sourcesEnc, jsonEnc)

    val writer = new CoverallPayloadWriter(
      projectBaseDir.value,
      coverallsFile.value,
      repoToken,
      travisJobIdent,
      coverallsServiceName.value,
      new GitClient(coverallsGitRepoLocation.value getOrElse ".")(log),
      sourcesEnc,
      jsonEnc
    )

    writer.start(log)

    val report = CoberturaFile(coberturaFile.value, baseDirectory.value)
    if (!report.exists) {
      sys.error("Could not find the cobertura.xml file. Did you call coverageAggregate?")
    }

    // include all of the sources (stanard roots and multi-module roots)
    val sources: Seq[File] = (sourceDirectories in Compile).value
    val multiSources: Seq[File] = coverallsSourceRoots.value.flatten
    val allSources = sources ++ multiSources

    val reader = new CoberturaMultiSourceReader(report.file, allSources, sourcesEnc)
    val sourceFiles = reader.sourceFilenames

    sourceFiles.foreach(sourceFile => {
      val sourceReport = reader.reportForSource(sourceFile)
      writer.addSourceFile(sourceReport)
    })

    writer.end()

    val res = coverallsClient.postFile(coverallsFile.value)
    val failBuildOnError = coverallsFailBuildOnError.value

    if (res.error) {
      val errorMessage =
        s"""
           |Uploading to $endpoint failed: ${res.message}
           |${
          if (res.message.contains(CoverallsClient.tokenErrorString))
            s"The error message '${CoverallsClient.tokenErrorString}' can mean your repo token is incorrect."
          else ""
        }
         """.stripMargin
      if (failBuildOnError)
        sys.error(errorMessage)
      else
        log.error(errorMessage)
    } else {
      log.info(s"Uploading to $endpoint succeeded: " + res.message)
      log.info(res.url)
      log.info("(results may not appear immediately)")
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
      case _: Exception => None
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
