package com.github.theon.coveralls

import java.io.{ File, StringWriter, Writer }

import com.fasterxml.jackson.core.JsonFactory
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import org.scoverage.coveralls.GitClient.GitRevision
import org.scoverage.coveralls.{ CoverallPayloadWriter, GitClient, SourceFileReport }
import sbt.ConsoleLogger

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

  def coverallsWriter(writer: Writer, tokenIn: Option[String], jobId: Option[String], serviceName: Option[String], pullRequest: Option[String], parallel: Boolean, enc: Option[String]) =
    new CoverallPayloadWriter(new File("").getAbsoluteFile, new File(""), tokenIn, jobId, serviceName, pullRequest, parallel, enc, testGitClient) {
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
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), Some("testTravisJob"), Some("travis-ci"), None, parallel = false, Some("UTF-8"))

        coverallsW.start
        coverallsW.flush()

        w.toString should equal(
          """{"repo_token":"testRepoToken","service_name":"travis-ci","service_job_id":"testTravisJob","parallel":false,""" +
            expectedGit +
            ""","source_files":["""
        )
      }

      "generate a correct starting payload without travis job id" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), None, None, None, parallel = false, Some("UTF-8"))

        coverallsW.start
        coverallsW.flush()

        w.toString should equal(
          """{"repo_token":"testRepoToken","parallel":false,""" +
            expectedGit +
            ""","source_files":["""
        )
      }

      "add source files correctly" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), None, Some("travis-ci"), None, parallel = false, Some("UTF-8"))

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
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), None, Some("travis-ci"), None, parallel = false, Some("UTF-8"))

        coverallsW.start
        coverallsW.end()

        w.toString should endWith("]}")
      }

      "generate a correct starting payload with circle job id" in {
        val w = new StringWriter()
        val coverallsW = coverallsWriter(w, Some("testRepoToken"), Some("12345"), Some("circle-ci"), Some("100"), parallel = true, Some("UTF-8"))

        coverallsW.start
        coverallsW.flush()

        w.toString should equal(
          """{"repo_token":"testRepoToken","service_name":"circle-ci","service_job_id":"12345","service_pull_request":"100","parallel":true,""" +
            expectedGit +
            ""","source_files":["""
        )
      }
    }
  }
}
