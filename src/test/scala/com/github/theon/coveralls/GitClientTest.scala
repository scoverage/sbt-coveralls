package com.github.theon.coveralls

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import sys.process._
import sbt.{ConsoleLogger}
import org.scalatest.matchers.ShouldMatchers
import org.scoverage.coveralls.GitClient

/**
 * Date: 30/03/2013
 * Time: 09:32
 */
class GitClientTest extends WordSpec with BeforeAndAfterAll with ShouldMatchers {

  implicit val log = ConsoleLogger(System.out)

  val git = new GitClient {
    //Repo generated in make_test_git_repo.sh
    override def cwd = Some("/tmp/xsbt-coveralls-plugin/test_repo")
  }

  override def beforeAll {
    "src/test/resources/make_test_git_repo.sh" !
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
        git.lastCommit("%H") should fullyMatch regex ("[0-9a-f]{40}")
      }
      "return a valid author name" in {
        git.lastCommit("%an") should equal("test_username")
      }
      "return a valid committer name" in {
        git.lastCommit("%cn") should equal("test_username")
      }
      "return a valid author email" in {
        git.lastCommit("%ae") should equal("test_user@test_email.com")
      }
      "return a valid committer email" in {
        git.lastCommit("%ce") should equal("test_user@test_email.com")
      }
      "return a valid author commit message" in {
        git.lastCommit("%s") should equal("Commit message for unit test")
      }
    }
  }
}
