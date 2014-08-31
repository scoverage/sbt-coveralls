package org.scoverage.coveralls

import com.fasterxml.jackson.core.JsonEncoding
import sbt.Keys._
import sbt._
import scala.io.{Codec, Source}
import java.io.File

object CoverallsPlugin extends Plugin with AbstractCoverallsPlugin {

  import CoverallsKeys._

  def apiHttpClient = new ScalaJHttpClient

  def travisJobIdent = sys.env.get("TRAVIS_JOB_ID")

  def userRepoToken(coverallsToken: Option[String], coverallsTokenFile: Option[String]) =
    sys.env.get("COVERALLS_REPO_TOKEN")
      .orElse(coverallsToken)
      .orElse(coverallsTokenFile.flatMap(repoTokenFromFile))

  object CoverallsKeys {
    //So people can configure coverallsToken := "string" even though it is a SettingKey[Option[String]]
    //Better way to do this?
    implicit def strToOpt(s: String) = Option(s)

    val coverallsTask = TaskKey[Unit]("coveralls", "Generate coveralls reports")
    val coverallsFile = SettingKey[File]("coveralls-file")
    val coverallsToken = SettingKey[Option[String]]("coveralls-repo-token")
    val coverallsTokenFile = SettingKey[Option[String]]("coveralls-token-file")
    val coverallsServiceName = SettingKey[Option[String]]("coveralls-service-name")
    val coverallsCoverageTask = SettingKey[String]("coveralls-coverage-task")

    val coberturaFile = SettingKey[File]("cobertura-file")
    val childCoberturaFilesTask = TaskKey[Seq[CoberturaFile]]("child-cobertura-files", "Finds all the cobertura files in the sub projects")

    val encoding = SettingKey[String]("encoding")
  }

  lazy val singleProject = coverallsSettings

  lazy val coverallsSettings: Seq[Setting[_]] = Seq (
    encoding := "UTF-8",
    coverallsToken := None,
    coverallsTokenFile := None,
    coverallsServiceName := travisJobIdent map { _ => "travis-ci" },
    coverallsFile <<= crossTarget / "coveralls.json",
    coverallsCoverageTask := "scoverage:test",
    coberturaFile <<= crossTarget / ("coverage-report" + File.separator + "cobertura.xml"),
    childCoberturaFilesTask <<= (thisProjectRef, buildStructure) map childCoberturaFiles,
    coverallsTask <<= (
      state,
      baseDirectory,
      coberturaFile,
      childCoberturaFilesTask,
      coverallsFile,
      encoding,
      coverallsToken,
      coverallsTokenFile,
      coverallsServiceName,
      coverallsCoverageTask
    ) map coverallsCommand
  )
}

case class CoberturaFile(file: File, projectBase: File) {
  def exists = file.exists
}

trait AbstractCoverallsPlugin  {

  def apiHttpClient: HttpClient

  def childCoberturaFiles(projectRef: ProjectRef, structure: BuildStructure) = {
    val subProjects = aggregated(projectRef, structure)
    subProjects flatMap { p =>
      val crossTargetOpt = (crossTarget in LocalProject(p)).get(structure.data)
      val baseDir = (baseDirectory in LocalProject(p)).get(structure.data)

      crossTargetOpt.map { crossTarg =>
        val coberFile = new File(crossTarg + File.separator + "coverage-report" + File.separator + "cobertura.xml")
        CoberturaFile(coberFile, baseDir.get)
      }
    }
  }

  def coverallsCommand(state: State,
                        rootProjectDir: File,
                        rootCoberturaFile: File,
                        childCoberturaFiles: Seq[CoberturaFile],
                        coverallsFile: File,
                        encoding: String,
                        coverallsToken: Option[String],
                        coverallsTokenFile: Option[String],
                        coverallsServiceName: Option[String],
                        coverallsCoverageTask: String): State = {
    var currState = state
    val repoToken = userRepoToken(coverallsToken, coverallsTokenFile)

    if(travisJobIdent.isEmpty && repoToken.isEmpty) {
      currState.log.error("Could not find coveralls repo token or determine travis job id")
      currState.log.error(" - If running from travis, make sure the TRAVIS_JOB_ID env variable is set")
      currState.log.error(" - Otherwise, to set up your repo token read https://github" +
        ".com/scoverage/sbt-coveralls#specifying-your-repo-token")
      return currState.fail
    }

    //Run the code coverage plugin to generate code coverage
    currState = Command.process(coverallsCoverageTask, currState)

    //Users can encode their source files in whatever encoding they desire, however when we send their source code to
    //the coveralls API, it is a JSON payload. RFC4627 states that JSON must be UTF encoded.
    //See http://tools.ietf.org/html/rfc4627#section-3
    val sourcesEnc = Codec(encoding)
    val jsonEnc = JsonEncoding.UTF8

    val coverallsClient = new CoverallsClient(apiHttpClient, sourcesEnc, jsonEnc)

    val writer = new CoverallPayloadWriter (
      coverallsFile,
      repoToken,
      travisJobIdent,
      coverallsServiceName,
      new GitClient(".")(currState.log),
      sourcesEnc,
      jsonEnc
    )

    writer.start(currState.log)

    val allCoberturaFiles =
      (CoberturaFile(rootCoberturaFile, rootProjectDir) +: childCoberturaFiles).filter(_.exists)

    if(allCoberturaFiles.isEmpty) {
      currState.log.error("Could not find any cobertura.xml files. Has the coverage plugin run?")
      return currState.fail
    }

    allCoberturaFiles.foreach(coberturaFile => {
      val reader = new CoberturaReader(coberturaFile.file, coberturaFile.projectBase, rootProjectDir, sourcesEnc)
      val sourceFiles = reader.sourceFilenames

      sourceFiles.foreach(sourceFile => {
        val sourceReport = reader.reportForSource(sourceFile)
        writer.addSourceFile(sourceReport)
      })
    })

    writer.end()

    val res = coverallsClient.postFile(coverallsFile)
    if(res.error) {
      currState.log.error("Uploading to coveralls.io failed: " + res.message)
      if(res.message.contains(CoverallsClient.buildErrorString)) {
        currState.log.error("The error message 'Build processing error' can mean your repo token is incorrect. See https://github.com/lemurheavy/coveralls-public/issues/46")
      } else {
        currState.log.error("Coveralls.io server internal error: " + res.message)
      }
      currState.fail
    } else {
      currState.log.info("Uploading to coveralls.io succeeded: " + res.message)
      currState.log.info(res.url)
      currState.log.info("(results may not appear immediately)")
      currState
    }
  }

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
