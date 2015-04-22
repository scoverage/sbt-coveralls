package com.github.theon.coveralls

import java.io.File

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import sbt.ConsoleLogger
import org.eclipse.jgit.api.Git
import org.scalatest.Matchers
import org.scoverage.coveralls.GitClient

class GitClientTest extends WordSpec with BeforeAndAfterAll with Matchers {

  implicit val log = ConsoleLogger(System.out)

  var git: GitClient = null

  override def beforeAll(): Unit = {
    // Create local repository
    val repoDir = File.createTempFile("test_repo", "")
    repoDir.delete()
    repoDir.mkdirs()
    val gitRepo = Git.init().setDirectory(repoDir).call()
    // Add two remotes
    val storedConfig = gitRepo.getRepository.getConfig
    storedConfig.setString("remote", "origin_test_1", "url", "git@origin_test_1")
    storedConfig.setString("remote", "origin_test_2", "url", "git@origin_test_2")
    storedConfig.save()
    // Add and commit a file
    val readme = new File(repoDir, "README.md")
    readme.createNewFile();
    gitRepo.add()
        .addFilepattern("README.md")
        .call()
    gitRepo.commit()
        .setAuthor("test_username", "test_user@test_email.com")
        .setCommitter("test_username", "test_user@test_email.com")
        .setMessage("Commit message for unit test")
        .call()

    git = new GitClient(repoDir.getAbsolutePath)
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
