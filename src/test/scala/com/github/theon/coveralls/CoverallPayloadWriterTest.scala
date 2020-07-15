package com.github.theon.coveralls

import java.io.{ File, StringWriter, Writer }

import com.fasterxml.jackson.core.JsonFactory
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scoverage.coveralls.GitClient.GitRevision
import org.scoverage.coveralls.{ CIService, CoverallPayloadWriter, GitClient, SourceFileReport, TravisCI }
import sbt.ConsoleLogger

class CoverallPayloadWriterTest extends AnyWordSpec with BeforeAndAfterAll with Matchers {

  implicit val log = ConsoleLogger(System.out)

  val testGitClient = new GitClient(new File(".")) {
    override def remotes = List("remote")
    override def remoteUrl(remoteName: String) = "remoteUrl"
    override def currentBranch = "branch"
    override def lastCommit(): GitRevision = {
      GitRevision("lastCommitId", "authorName", "authorEmail", "committerName", "committerEmail", "shortMsg")
    }
  }

  def coverallsWriter(writer: Writer, tokenIn: Option[String], service: Option[CIService]) =
    new CoverallPayloadWriter(new File("").getAbsoluteFile, new File(""), tokenIn, service, testGitClient) {
      override def generator(file: File) = {
        val factory = new JsonFactory()
        factory.createGenerator(writer)
      }
    }

  val expectedGit = """"git":{"head":{"id":"lastCommitId","author_name":"authorName","author_email":"authorEmail","committer_name":"committerName","committer_email":"committerEmail","message":"shortMsg"},"branch":"branch","remotes":[{"name":"remote","url":"remoteUrl"}]}"""

  "CoverallPayloadWriter" when {
    "generating coveralls API payload" should {

      "generate a correct starting payload with a job id from a CI service" in {
        val testService: CIService = new CIService {
          override def name = "my-service"
          override def jobId = Some("testServiceJob")
          override def pullRequest = None
          override def currentBranch = None
        }

        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), Some(testService))

        coverallsW.start
        coverallsW.flush()

        w.toString should equal(
          """{"repo_token":"testRepoToken","service_name":"my-service","service_job_id":"testServiceJob",""" +
            expectedGit +
            ""","source_files":["""
        )
      }

      "generate a correct starting payload without a CI service" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), None)

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
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), Some(TravisCI))

        val projectRoot = new File("").getAbsolutePath.replace(File.separator, "/") + "/"

        coverallsW.addSourceFile(
          SourceFileReport(projectRoot + "src/test/resources/TestSourceFile.scala", List(Some(1), None, Some(2)))
        )
        coverallsW.flush()

        w.toString should equal(
          """{"name":"src/test/resources/TestSourceFile.scala","source_digest":"A420A88E114B1CB1272F5984F333C75C","coverage":[1,null,2]}"""
        )
      }

      "end the file correctly" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), Some(TravisCI))

        coverallsW.start
        coverallsW.end()

        w.toString should endWith("]}")
      }
    }
  }
}
