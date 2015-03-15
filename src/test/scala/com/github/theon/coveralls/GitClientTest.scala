package com.github.theon.coveralls

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import sys.process._
import sbt.ConsoleLogger
import org.scalatest.Matchers
import org.scoverage.coveralls.GitClient

/**
 * Date: 30/03/2013
 * Time: 09:32
 */
class GitClientTest extends WordSpec with BeforeAndAfterAll with Matchers {

  implicit val log = ConsoleLogger(System.out)

  var git: GitClient = null

  override def beforeAll() :Unit = {
    "src/test/resources/make_test_git_repo.sh".!
    git = new GitClient("/tmp/xsbt-coveralls-plugin/test_repo")
  }

  "GitClient" when {

    "asked for remotes" should {
      "return a valid response" in {
        git.remotes should contain ("origin_test_1")
        git.remotes should contain ("origin_test_2")
      }
    }

    "asked for a remote's url" should {
      "return a valid response" in {
        git.remoteUrl("origin_test_1") should equal ("git@origin_test_1")
        git.remoteUrl("origin_test_2") should equal ("git@origin_test_2")
      }
    }

    "asked for the current branch" should {
      "return a valid response" in {
        git.currentBranch should equal ("master")
      }
    }

    "asked for the last commit" should {
      "return a valid hash" in {
        git.lastCommit().id should fullyMatch regex "[0-9a-f]{40}"
      }
      "return a valid author name" in {
        git.lastCommit().authorName should equal("test_username")
      }
      "return a valid committer name" in {
        git.lastCommit().committerName should equal("test_username")
      }
      "return a valid author email" in {
        git.lastCommit().authorEmail should equal("test_user@test_email.com")
      }
      "return a valid committer email" in {
        git.lastCommit().committerEmail should equal("test_user@test_email.com")
      }
      "return a valid author commit message" in {
        git.lastCommit().shortMessage should equal("Commit message for unit test")
      }
    }
  }
}
