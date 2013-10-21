import com.fasterxml.jackson.core.JsonEncoding
import com.github.theon.coveralls._
import sbt.Keys._
import sbt._
import scala.io.{Codec, Source}
import java.io.File

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

    val coberturaFile = SettingKey[File]("cobertura-file")
    val coverallsFile = SettingKey[File]("coveralls-file")

    val childCoberturaFilesTask = TaskKey[Seq[CoberturaFile]]("child-cobertura-files", "Finds all the cobertura files in the sub projects")

    val coverallsTask = TaskKey[Unit]("coveralls", "Generate coveralls reports")
    val encoding = SettingKey[String]("encoding")
    val coverallsToken = SettingKey[Option[String]]("coveralls-repo-token")
    val coverallsTokenFile = SettingKey[Option[String]]("coveralls-token-file")
    val coverallsServiceName = SettingKey[Option[String]]("coveralls-service-name")
  }

  lazy val singleProject = ScctPlugin.instrumentSettings ++ coverallsSettings

  lazy val multiProject = ScctPlugin.mergeReportSettings ++ coverallsSettings

  lazy val coverallsSettings: Seq[Setting[_]] = Seq (
    encoding := "UTF-8",
    coverallsToken := None,
    coverallsTokenFile := None,
    coverallsServiceName := travisJobIdent map { _ => "travis-ci" },
    coverallsFile <<= crossTarget / "coveralls.json",
    coberturaFile <<= crossTarget / ("coverage-report" + File.separator + "/cobertura.xml"),
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
      coverallsServiceName
    ) map coverallsCommand
  )
}

case class CoberturaFile(file: File, projectBase: File) {
  def exists = file.exists
}

trait AbstractCoverallsPlugin  {

  def apiHttpClient: HttpClient

  def childCoberturaFiles(projectRef: ProjectRef, structure: Load.BuildStructure) = {
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
                        coverallsServiceName: Option[String]): State = {

    val repoToken = userRepoToken(coverallsToken, coverallsTokenFile)

    if(travisJobIdent.isEmpty && repoToken.isEmpty) {
      state.log.error("Could not find coveralls repo token or determine travis job id")
      state.log.error(" - If running from travis, make sure the TRAVIS_JOB_ID env variable is set")
      state.log.error(" - Otherwise, to set up your repo token read https://github.com/theon/xsbt-coveralls-plugin#specifying-your-repo-token")
      return state.fail
    }

    //Run the scct plugin to generate code coverage
    Command.process("scct:test", state)

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
      new GitClient,
      sourcesEnc,
      jsonEnc
    )

    writer.start(state.log)

    val allCoberturaFiles =
      (CoberturaFile(rootCoberturaFile, rootProjectDir) +: childCoberturaFiles).filter(_.exists)

    if(allCoberturaFiles.isEmpty) {
      state.log.error("Could not find any cobertura.xml files. Has SCCT run?")
      return state.fail
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
      state.log.error("Uploading to coveralls.io failed: " + res.message)
      if(res.message.contains("Build processing error")) {
        state.log.error("The error message 'Build processing error' can mean your repo token is incorrect. See https://github.com/lemurheavy/coveralls-public/issues/46")
      }
      state.fail
    } else {
      state.log.info("Uploading to coveralls.io succeeded: " + res.message)
      state.log.info(res.url)
      state.log.info("(results may not appear immediately)")
      state
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

  def aggregated(projectRef: ProjectRef, structure: Load.BuildStructure): Seq[String] = {
    val aggregate = Project.getProject(projectRef, structure).toSeq.flatMap(_.aggregate)
    aggregate flatMap { ref =>
      ref.project +: aggregated(ref, structure)
    }
  }

  @deprecated("Add this to your build.sbt instead: coverallsTokenFile := \"path/to/file/with/my/token.txt\"", "June 2013")
  def userRepoTokenFromLegacyFile =
    repoTokenFromFile(Path.userHome.getAbsolutePath + "/.sbt/coveralls.repo.token")
}
