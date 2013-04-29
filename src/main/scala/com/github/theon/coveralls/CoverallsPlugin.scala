package com.github.theon.coveralls

import sbt.Keys._
import sbt._
import io.Source
import tools.nsc.Global
import annotation.tailrec

/**
 * Date: 10/03/2013
 * Time: 17:01
 */
object CoverallsPlugin extends AbstractCoverallsPlugin {
  def coberturaFile(state:State) = findCoberturaFile("target/", state).head
  def coverallsFile(state:State) = baseDir(state) + "target/coveralls.json"
  def apiHttpClient = new ScalaJHttpClient
  def baseDir(state:State) = state.configuration.baseDirectory.getAbsolutePath + "/"

  def travisJobIdent = sys.env.get("TRAVIS_JOB_ID")

  def userRepoToken =
    sys.env.get("COVERALLS_REPO_TOKEN").orElse(userRepoTokenFromFile)

  //TODO: Get rid of this awful method. I make myself sick.
  def findCoberturaFile(path:String, state:State):Set[String] = {
    val f = new File(path)
    val children = Option[Array[File]](f.listFiles())
    if(f.name == "cobertura.xml") Set(f.getAbsolutePath)
    else if(children.isEmpty) Set.empty
    else children.get.foldLeft(Set[String]())((set, file) => set ++ findCoberturaFile(file.getAbsolutePath, state))
  }
}
trait AbstractCoverallsPlugin extends Plugin {

  override lazy val settings = Seq(commands += Command.args("coveralls", "test")(coverallsCommand))

  def coberturaFile(state:State):String
  def coverallsFile(state:State):String
  def baseDir(state:State):String

  def apiHttpClient:HttpClient

  def scctConfig = config("scct-test")

  def coverallsCommand = (state:State, args:Seq[String]) => {
    if(travisJobIdent.isEmpty && userRepoToken.isEmpty) {
      state.log.error("Could not find coveralls repo token or determine travis job id")
      state.log.error(" - If running from travis, make sure the TRAVIS_JOB_ID env variable is set")
      state.log.error(" - Otherwise, make sure the COVERALLS_REPO_TOKEN env variable is set")
      state.fail
    } else {
      //Run the scct plugin to generate code coverage
      Command.process("scct:test", state)

      val reader = new CoberturaReader {
        def file = coberturaFile(state)
      }

      val writer = new CoverallPayloadWriter {
        def repoToken = userRepoToken
        def file = coverallsFile(state)
        def travisJobId = travisJobIdent
        val gitClient = new GitClient {}
      }

      val coverallsClient = new CoverallsClient {
        def httpClient = apiHttpClient
      }
      val sourceFiles = reader.sourceFilenames()
      writer.start(state.log)

      sourceFiles.foreach(sourceFile => {
        val sourceReport = reader.reportForSource(baseDir(state), sourceFile)
        writer.addSourceFile(sourceReport)
      })

      writer.end()

      val res = coverallsClient.postFile(coverallsFile(state))
      if(res.error) {
        state.log.error("Uploading to coveralls.io failed: " + res.message)
        state.fail
      } else {
        state.log.info("Uploading to coveralls.io succeeded: " + res.message)
        state.log.info(res.url)
        state
      }
    }
  }

  def travisJobIdent:Option[String]
  def userRepoToken:Option[String]

  def userRepoTokenFromFile = {
    try {
      Some(Source.fromFile(Path.userHome.getAbsolutePath + "/.sbt/coveralls.repo.token").mkString)
    } catch {
      case e:Exception => None
    }
  }
}