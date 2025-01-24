package org.scoverage.coveralls

import sbt.Keys._
import sbt.internal.util.ManagedLogger
import sbt.{ScopeFilter, ThisProject, _}

import java.io.File
import scala.io.{BufferedSource, Source}
import scala.util.control.NonFatal

object Imports {
  object CoverallsKeys {
    val coverallsFile = SettingKey[File]("coverallsFile")
    val coverallsToken = SettingKey[Option[String]]("coverallsRepoToken")
    val coverallsTokenFile = SettingKey[Option[String]]("coverallsTokenFile")
    val coverallsService = SettingKey[Option[CIService]]("coverallsService")
    val coverallsFailBuildOnError = SettingKey[Boolean](
      "coverallsFailBuildOnError",
      "fail build if coveralls step fails"
    )
    val coberturaFile = SettingKey[File]("coberturaFile")
    @deprecated(
      "Read https://github.com/scoverage/sbt-coveralls#custom-source-file-encoding",
      "1.2.5"
    )
    val coverallsEncoding = SettingKey[String]("encoding")
    val coverallsEndpoint = SettingKey[Option[String]]("coverallsEndpoint")
    val coverallsGitRepoLocation =
      SettingKey[Option[String]]("coveralls-git-repo")
    val coverallsParallel = SettingKey[Boolean]("coverallsParallel")
  }
}

object CoverallsPlugin extends AutoPlugin {
  override def trigger = allRequirements

  val autoImport = Imports
  import autoImport.*
  import CoverallsKeys.*

  lazy val coveralls = taskKey[Unit](
    "Uploads scala code coverage to coveralls.io"
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    coveralls := coverallsTask.value,
    coveralls / aggregate := false,
    coverallsFailBuildOnError := false,
    coverallsToken := None,
    coverallsTokenFile := None,
    coverallsEndpoint := Some("https://coveralls.io"),
    coverallsService := {
      if (travisJobIdent.isDefined) Some(TravisCI)
      else if (githubActionsRunIdent.isDefined) Some(GitHubActions)
      else None
    },
    coverallsFile := crossTarget.value / "coveralls.json",
    coberturaFile := crossTarget.value / "coverage-report" / "cobertura.xml",
    coverallsGitRepoLocation := Some("."),
    coverallsParallel := sys.env.get("COVERALLS_PARALLEL").contains("true")
  )

  val aggregateFilter = ScopeFilter(
    inAggregates(ThisProject),
    inConfigurations(Compile)
  ) // must be outside of the 'coverageAggregate' task (see: https://github.com/sbt/sbt/issues/1095 or https://github.com/sbt/sbt/issues/780)

  def coverallsTask = Def.task {
    implicit val log: ManagedLogger = streams.value.log

    if (!coberturaFile.value.exists) {
      sys.error(
        "Could not find the cobertura.xml file. Did you call coverageAggregate?"
      )
    }

    val repoToken =
      userRepoToken(coverallsToken.value, coverallsTokenFile.value)

    val coverallsAuthOpt = coverallsService.value match {
      case Some(ciService) => ciService.coverallsAuth(repoToken)
      case None            => repoToken.map(CoverallsRepoToken)
    }

    val coverallsAuth = coverallsAuthOpt.getOrElse {
      sys.error("""
          |Could not find any way to authenticate against Coveralls.
          | - If running from Travis, make sure the TRAVIS_JOB_ID env variable is set
          | - If running from GitHub CI, set the GITHUB_TOKEN env variable to ${{ secrets.GITHUB_TOKEN }}
          | - Otherwise, to set up your repo token read https://github.com/scoverage/sbt-coveralls#specifying-your-repo-token
        """.stripMargin)
    }

    val sourcesEnc = sourceEncoding((Compile / scalacOptions).value)

    val endpoint = userEndpoint(coverallsEndpoint.value).get

    val coverallsClient = new CoverallsClient(endpoint, apiHttpClient)

    val repoRootDirectory =
      new File(coverallsGitRepoLocation.value getOrElse ".")

    val writer = new CoverallPayloadWriter(
      repoRootDirectory,
      coverallsFile.value,
      coverallsAuth,
      coverallsService.value,
      coverallsParallel.value,
      new GitClient(repoRootDirectory)
    )

    writer.start()

    // include all of the sources (from all modules)
    val allSources = sourceDirectories
      .all(aggregateFilter)
      .value
      .flatten
      .filter(_.isDirectory())
      .distinct

    val reader = new CoberturaMultiSourceReader(
      coberturaFile.value,
      allSources,
      sourcesEnc
    )

    log.info(
      s"sbt-coveralls: Generating reports for ${reader.sourceFilenames.size} files ..."
    )

    val fileReports =
      reader.sourceFilenames.par.map(reader.reportForSource).seq

    log.info(
      s"sbt-coveralls: Adding file reports to the coveralls file (${coverallsFile.value.getName}) ..."
    )

    fileReports.foreach(writer.addSourceFile)

    writer.end()

    log.info(
      s"sbt-coveralls: Uploading the coveralls file (${coverallsFile.value.getName}) ..."
    )

    val res = coverallsClient.postFile(coverallsFile.value)
    val failBuildOnError = coverallsFailBuildOnError.value

    if (res.error) {
      val errorMessage =
        s"""
           |Uploading to $endpoint failed: ${res.message}
           |${if (res.message.contains(CoverallsClient.tokenErrorString))
            s"The error message '${CoverallsClient.tokenErrorString}' can mean your repo token is incorrect."
          else ""}
         """.stripMargin
      if (failBuildOnError)
        sys.error(errorMessage)
      else
        log.error(errorMessage)
    } else {
      log.info(
        s"sbt-coveralls: Uploading to $endpoint succeeded (results may not appear immediately): ${res.message}/${res.url}"
      )
    }
  }

  def apiHttpClient: ScalaJHttpClient = new ScalaJHttpClient

  def travisJobIdent: Option[String] = sys.env.get("TRAVIS_JOB_ID")

  def githubActionsRunIdent: Option[String] = sys.env.get("GITHUB_RUN_ID")

  def repoTokenFromFile(path: String): Option[String] = {
    var source: BufferedSource = null
    try {
      source = Source.fromFile(path)
      val repoToken = source.mkString.trim
      source.close()
      Some(repoToken)
    } catch {
      case NonFatal(_) => None
    } finally {
      if (source != null)
        source.close()
    }
  }

  def userRepoToken(
      coverallsToken: Option[String],
      coverallsTokenFile: Option[String]
  ): Option[String] =
    sys.env
      .get("COVERALLS_REPO_TOKEN")
      .orElse(coverallsToken)
      .orElse(coverallsTokenFile.flatMap(repoTokenFromFile))

  def userEndpoint(coverallsEndpoint: Option[String]): Option[String] =
    sys.env
      .get("COVERALLS_ENDPOINT")
      .orElse(coverallsEndpoint)

  private def sourceEncoding(scalacOptions: Seq[String]): Option[String] = {
    val i = scalacOptions.indexOf("-encoding") + 1
    if (i > 0 && i < scalacOptions.length) Some(scalacOptions(i)) else None
  }
}
