package com.github.theon.coveralls

import java.io.{ File, StringWriter, Writer }

import com.fasterxml.jackson.core.{ JsonEncoding, JsonFactory }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import org.scoverage.coveralls.GitClient.GitRevision
import org.scoverage.coveralls.{ CoverallPayloadWriter, GitClient, SourceFileReport }
import sbt.ConsoleLogger

import scala.io.Codec

class CoverallPayloadWriterTest extends WordSpec with BeforeAndAfterAll with Matchers {

  implicit val log = ConsoleLogger(System.out)

  val testGitClient = new GitClient(new File(".")) {
    override def remotes = List("remote")
    override def remoteUrl(remoteName: String) = "remoteUrl"
    override def currentBranch = "branch"
    override def lastCommit(): GitRevision = {
      GitRevision("lastCommitId", "authorName", "authorEmail", "committerName", "committerEmail", "shortMsg")
    }
  }

  def coverallsWriter(writer: Writer, tokenIn: Option[String], travisJobIdIn: Option[String], serviceName: Option[String], enc: Codec) =
    new CoverallPayloadWriter(new File("").getAbsoluteFile, new File(""), tokenIn, travisJobIdIn, serviceName, testGitClient, enc, JsonEncoding.UTF8) {
      override def generator(file: File) = {
        val factory = new JsonFactory()
        factory.createGenerator(writer)
      }
    }

  val expectedGit = """"git":{"head":{"id":"lastCommitId","author_name":"authorName","author_email":"authorEmail","committer_name":"committerName","committer_email":"committerEmail","message":"shortMsg"},"branch":"branch","remotes":[{"name":"remote","url":"remoteUrl"}]}"""

  "CoverallPayloadWriter" when {
    "generating coveralls API payload" should {

      "generate a correct starting payload with travis job id" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), Some("testTravisJob"), Some("travis-ci"), Codec("UTF-8"))

        coverallsW.start
        coverallsW.flush()

        w.toString should equal(
          """{"repo_token":"testRepoToken","service_name":"travis-ci","service_job_id":"testTravisJob",""" +
            expectedGit +
            ""","source_files":["""
        )
      }

      "generate a correct starting payload without travis job id" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), None, None, Codec("UTF-8"))

        coverallsW.start
        coverallsW.flush()

        w.toString should equal(
          """{"repo_token":"testRepoToken",""" +
            expectedGit +
            ""","source_files":["""
        )
      }

      "add source files correctly" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), None, Some("travis-ci"), Codec("UTF-8"))

        val projectRoot = new File("").getAbsolutePath.replace(File.separator, "/") + "/"

        coverallsW.addSourceFile(
          SourceFileReport(projectRoot, projectRoot + "src/test/resources/TestSourceFile.scala", List(Some(1), None, Some(2)))
        )
        coverallsW.flush()

        w.toString should equal(
          """{"name":"src/test/resources/TestSourceFile.scala","source":"/**\n * Test Scala Source File that is 10 lines\n */\nclass TestSourceFile {\n\n\n\n\n\n}","coverage":[1,null,2]}"""
        )
      }

      "end the file correctly" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), None, Some("travis-ci"), Codec("UTF-8"))

        coverallsW.start
        coverallsW.end()

        w.toString should endWith("]}")
      }
    }
  }
}
