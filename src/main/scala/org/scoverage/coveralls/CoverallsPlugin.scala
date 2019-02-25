package org.scoverage.coveralls

import _root_.sbt.ScopeFilter
import _root_.sbt.ThisProject
import com.fasterxml.jackson.core.JsonEncoding
import sbt.Keys._
import sbt._

import scala.io.Source
import java.io.File

object Imports {
  object CoverallsKeys {
    val coverallsFile = SettingKey[File]("coverallsFile")
    val coverallsToken = SettingKey[Option[String]]("coverallsRepoToken")
    val coverallsTokenFile = SettingKey[Option[String]]("coverallsTokenFile")
    val coverallsServiceName = SettingKey[Option[String]]("coverallsServiceName")
    val coverallsFailBuildOnError = SettingKey[Boolean](
      "coverallsFailBuildOnError", "fail build if coveralls step fails")
    val coberturaFile = SettingKey[File]("coberturaFile")
    @deprecated("Read https://github.com/scoverage/sbt-coveralls#custom-source-file-encoding", "1.2.5")
    val coverallsEncoding = SettingKey[String]("encoding")
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
    aggregate in coveralls := false,
    coverallsFailBuildOnError := false,
    coverallsToken := None,
    coverallsTokenFile := None,
    coverallsEndpoint := Option("https://coveralls.io"),
    coverallsServiceName := {
      if (isCircle) {
        Some("circleci")
      } else {
        travisJobIdent map { _ => "travis-ci" }
      }
    },
    coverallsFile := crossTarget.value / "coveralls.json",
    coberturaFile := crossTarget.value / "coverage-report" / "cobertura.xml",
    coverallsGitRepoLocation := Some(".")
  )

  val aggregateFilter = ScopeFilter(inAggregates(ThisProject), inConfigurations(Compile)) // must be outside of the 'coverageAggregate' task (see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780)

  def coverallsTask = Def.task {
    if (!coberturaFile.value.exists) {
      sys.error("Could not find the cobertura.xml file. Did you call coverageAggregate?")
    }

    val repoToken = userRepoToken(coverallsToken.value, coverallsTokenFile.value)

    if (travisJobIdent.isEmpty && repoToken.isEmpty) {
      sys.error(
        """
          |Could not find coveralls repo token or determine travis job id
          | - If running from travis, make sure the TRAVIS_JOB_ID env variable is set
          | - Otherwise, to set up your repo token read https://github.com/scoverage/sbt-coveralls#specifying-your-repo-token
        """.stripMargin)
    }

    implicit val log = streams.value.log

    val sourcesEnc = sourceEncoding((scalacOptions in (Compile)).value)

    val endpoint = userEndpoint(coverallsEndpoint.value).get

    val coverallsClient = new CoverallsClient(endpoint, apiHttpClient)

    val repoRootDirectory = new File(coverallsGitRepoLocation.value getOrElse ".")

    val writer = new CoverallPayloadWriter(
      repoRootDirectory,
      coverallsFile.value,
      repoToken,
      coverallsServiceName.value,
      serviceNumber,
      jobId,
      circlePullRequest,
      isParallel,
      sourcesEnc,
      new GitClient(repoRootDirectory)(log)
    )

    writer.start()

    // include all of the sources (from all modules)
    val allSources = sourceDirectories.all(aggregateFilter).value.flatten.filter(_.isDirectory()).distinct

    val reader = new CoberturaMultiSourceReader(coberturaFile.value, allSources, sourcesEnc)

    log.info(s"Generating reports for ${reader.sourceFilenames.size} files")

    val fileReports = reader.sourceFilenames.par.map(reader.reportForSource(_)).seq

    log.info(s"Adding file reports to the coveralls file (${coverallsFile.value.getName})")

    fileReports.foreach(writer.addSourceFile(_))

    writer.end()

    log.info(s"Uploading the coveralls file (${coverallsFile.value.getName})")

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

  def isParallel: Boolean = sys.env.getOrElse("COVERALLS_PARALLEL", "") == "true"

  def isCircle: Boolean = sys.env.getOrElse("CIRCLECI", "") == "true"

  def circlePullRequest: Option[String] = sys.env.getOrElse("CI_PULL_REQUEST", "").split("/pull/").lift(1)

  def jobId: Option[String] = Some(sys.env.getOrElse("CIRCLE_BUILD_NUM", "") + sys.env.getOrElse("CIRCLE_NODE_INDEX", ""))

  def serviceNumber: Option[String] = if (isCircle) sys.env.get("CIRCLE_WORKFLOW_ID") else travisJobIdent

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


  private def sourceEncoding(scalacOptions: Seq[String]): Option[String] = {
    val i = scalacOptions.indexOf("-encoding") + 1
    if (i > 0 && i < scalacOptions.length) Some(scalacOptions(i)) else None
  }


}
