package org.scoverage.coveralls

import com.fasterxml.jackson.core.JsonEncoding
import sbt.Keys._
import sbt._
import scala.io.{Codec, Source}
import java.io.File

object CoverallsPlugin extends AutoPlugin with AbstractCoverallsPlugin {

  import CoverallsKeys._

  def apiHttpClient = new ScalaJHttpClient

  def travisJobIdent = sys.env.get("TRAVIS_JOB_ID")

  def userRepoToken(coverallsToken: Option[String], coverallsTokenFile: Option[String]) =
    sys.env.get("COVERALLS_REPO_TOKEN")
      .orElse(coverallsToken)
      .orElse(coverallsTokenFile.flatMap(repoTokenFromFile))

  object CoverallsKeys {
    // So people can configure coverallsToken := "string" even though it is a SettingKey[Option[String]]
    // Better way to do this?
    implicit def strToOpt(s: String): Option[String] = Option(s)

    val coveralls = taskKey[Unit]("Generate coveralls reports")
    val coverallsFile = settingKey[File]("coveralls-file")
    val coverallsToken = settingKey[Option[String]]("coveralls-repo-token")
    val coverallsTokenFile = settingKey[Option[String]]("coveralls-token-file")
    val coverallsServiceName = settingKey[Option[String]]("coverallsServiceName")
    val coverallsFailBuildOnError = settingKey[Boolean]("fail build if coveralls step fails")
    val coberturaFile = settingKey[File]("coberturaFile")
    val coverallsEncoding = settingKey[String]("encoding")
    val childCoberturaFiles = taskKey[Seq[CoberturaFile]]("Finds all the cobertura files in the sub projects")
  }

  lazy val singleProject = projectSettings

  override def trigger = allRequirements
  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    coverallsFailBuildOnError := false,
    coverallsEncoding := "UTF-8",
    coverallsToken := None,
    coverallsTokenFile := None,
    coverallsFailBuildOnError := false,
    coverallsServiceName := travisJobIdent map { _ => "travis-ci" },
    coverallsFile <<= crossTarget / "coveralls.json",
    coberturaFile <<= crossTarget / ("coverage-report" + File.separator + "cobertura.xml"),
    childCoberturaFiles := {
      val subProjects = aggregated(thisProjectRef.value, buildStructure.value)
      subProjects flatMap { p =>
        val crossTargetOpt = (crossTarget in LocalProject(p)).get(buildStructure.value.data)
        val baseDir = (baseDirectory in LocalProject(p)).get(buildStructure.value.data)
        crossTargetOpt.map { crossTarg =>
          val coberFile = new File(crossTarg + File.separator + "coverage-report" + File.separator + "cobertura.xml")
          CoberturaFile(coberFile, baseDir.get)
        }
      }
    },
    coveralls := {
      val s = streams.value
      val repoToken = userRepoToken(coverallsToken.value, coverallsTokenFile.value)

      if (travisJobIdent.isEmpty && repoToken.isEmpty) {
        s.log.error("Could not find coveralls repo token or determine travis job id")
        s.log.error(" - If running from travis, make sure the TRAVIS_JOB_ID env variable is set")
        s.log.error(
          " - Otherwise, to set up your repo token read https://github.com/scoverage/sbt-coveralls#specifying-your-repo-token")
        state.value.fail
      }

      //Users can encode their source files in whatever encoding they desire, however when we send their source code to
      //the coveralls API, it is a JSON payload. RFC4627 states that JSON must be UTF encoded.
      //See http://tools.ietf.org/html/rfc4627#section-3
      val sourcesEnc = Codec(coverallsEncoding.value)
      val jsonEnc = JsonEncoding.UTF8

      val coverallsClient = new CoverallsClient(apiHttpClient, sourcesEnc, jsonEnc)

      val writer = new CoverallPayloadWriter(
        coverallsFile.value,
        repoToken,
        travisJobIdent,
        coverallsServiceName.value,
        new GitClient(".")(s.log),
        sourcesEnc,
        jsonEnc
      )

      writer.start(s.log)

      val allCoberturaFiles =(CoberturaFile(coberturaFile.value, baseDirectory.value) +: childCoberturaFiles.value).filter(_.exists)

      if (allCoberturaFiles.isEmpty) {
        s.log.error("Could not find any cobertura.xml files. Has the coverage plugin run?")
      }

      allCoberturaFiles.foreach(coberturaFile => {
        val reader = new
            CoberturaReader(coberturaFile.file, coberturaFile.projectBase, baseDirectory.value, sourcesEnc)
        val sourceFiles = reader.sourceFilenames

        sourceFiles.foreach(sourceFile => {
          val sourceReport = reader.reportForSource(sourceFile)
          writer.addSourceFile(sourceReport)
        })
      })

      writer.end()

      val res = coverallsClient.postFile(coverallsFile.value)
      if (res.error) {
        s.log.error("Uploading to coveralls.io failed: " + res.message)
        if (res.message.contains(CoverallsClient.buildErrorString)) {
          s.log.error(
            "The error message 'Build processing error' can mean your repo token is incorrect. See https://github.com/lemurheavy/coveralls-public/issues/46")
        } else {
          s.log.error("Coveralls.io server internal error: " + res.message)
        }
        if (coverallsFailBuildOnError.value)
          state.value.fail
        else
          state
      } else {
        s.log.info("Uploading to coveralls.io succeeded: " + res.message)
        s.log.info(res.url)
        s.log.info("(results may not appear immediately)")
      }
    }
  )
}

case class CoberturaFile(file: File, projectBase: File) {
  def exists = file.exists
}

trait AbstractCoverallsPlugin  {

  def apiHttpClient: HttpClient

  def travisJobIdent: Option[String]
  def userRepoToken(coverallsToken: Option[String], coverallsTokenFile: Option[String]): Option[String]

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

  def aggregated(projectRef: ProjectRef, structure: BuildStructure): Seq[String] = {
    val aggregate = Project.getProject(projectRef, structure).toSeq.flatMap(_.aggregate)
    aggregate flatMap { ref =>
      ref.project +: aggregated(ref, structure)
    }
  }
}
