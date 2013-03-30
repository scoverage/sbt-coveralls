package com.github.theon.coveralls

import sbt.{ConsoleLogger, Logger}
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import org.codehaus.jackson.{JsonEncoding, JsonFactory}
import java.io.{StringWriter, Writer, File}

/**
 * Date: 30/03/2013
 * Time: 13:18
 */
class CoverallPayloadWriterTest extends WordSpec with BeforeAndAfterAll with ShouldMatchers {

  implicit val log = ConsoleLogger(System.out)

  val testGitClient = new GitClient {
    override def remotes(implicit log: Logger) = List("remote")
    override def remoteUrl(remoteName:String)(implicit log: Logger) = "remoteUrl"
    override def currentBranch(implicit log: Logger) = "branch"
    override def lastCommit(format:String)(implicit log: Logger) = "lastCommit"
  }

  def coverallsWriter(writer:Writer, tokenIn:String, travisJobIdIn:Option[String]) = new CoverallPayloadWriter {
    def file = ""
    def repoToken = tokenIn
    def travisJobId = travisJobIdIn
    val gitClient = testGitClient

    override def generator(file:String) = {
      val factory = new JsonFactory()
      factory.createJsonGenerator(writer)
    }
  }

  val expectedGit = """"git":{"head":{"id":"lastCommit","author_name":"lastCommit","author_email":"lastCommit","committer_name":"lastCommit","committer_email":"lastCommit","message":"lastCommit"},"branch":"branch","remotes":[{"name":"remote","url":"remoteUrl"}]}"""

  "CoverallPayloadWriter" when {
    "generating coveralls API payload" should {

      "generate a correct starting payload with travis job id" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, "testRepoToken", Some("testTravisJob"))

        coverallsW.start
        coverallsW.flush

        w.toString should equal (
          """{"repo_token":"testRepoToken","service_name":"travis-ci","service_job_id":"testTravisJob",""" +
          expectedGit +
          ""","source_files":["""
        )
      }

      "generate a correct starting payload without travis job id" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, "testRepoToken", None)

        coverallsW.start
        coverallsW.flush

        w.toString should equal (
          """{"repo_token":"testRepoToken",""" +
            expectedGit +
            ""","source_files":["""
        )
      }

      "add source files correctly" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, "testRepoToken", None)

        coverallsW.addSouceFile (
          SourceFileReport("src/test/resources/TestSourceFile.scala", List(Some(1), None, Some(2)))
        )
        coverallsW.flush

        w.toString should equal (
          """{"name":"src/test/resources/TestSourceFile.scala","source":"/**\n * Test Scala Source File that is 10 lines\n */\nclass TestSourceFile {\n\n\n\n\n\n}","coverage":[1,null,2]}"""
        )
      }

      "end the file correctly" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, "testRepoToken", None)

        coverallsW.start
        coverallsW.end

        w.toString should endWith ("]}")
      }
    }
  }
}
