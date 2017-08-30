package org.scoverage.coveralls

import java.io.File

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.scoverage.coveralls.GitClient.GitRevision
import sbt.Logger

object GitClient {
  case class GitRevision(
    id: String,
    authorName: String,
    authorEmail: String,
    committerName: String,
    committerEmail: String,
    shortMessage: String)
}

class GitClient(cwd: String)(implicit log: Logger) {

  import scala.collection.JavaConverters._

  val repository = FileRepositoryBuilder.create(new File(cwd, ".git"))
  val storedConfig = repository.getConfig
  log.info("Repository = " + repository.getDirectory)

  def remotes: Seq[String] = {
    storedConfig.getSubsections("remote").asScala.to[Seq]
  }

  def remoteUrl(remoteName: String): String = {
    storedConfig.getString("remote", remoteName, "url")
  }

  def currentBranch: String =
    repository.getBranch

  def lastCommit(): GitRevision = {
    val git = new Git(repository)
    val headRev = git.log().setMaxCount(1).call().asScala.head
    val id = headRev.getId
    val author = headRev.getAuthorIdent
    val committer = headRev.getCommitterIdent
    GitRevision(
      id.name,
      author.getName,
      author.getEmailAddress,
      committer.getName,
      committer.getEmailAddress,
      headRev.getShortMessage
    )
  }
}
