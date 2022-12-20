package org.scoverage.coveralls

import java.io.File
import java.nio.file.Files.lines

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
      shortMessage: String
  )
}

class GitClient(cwd: File)(implicit log: Logger) {

  import scala.collection.JavaConverters._

  val gitDirLineRegex = """^gitdir: (.*)""".r

  val gitFile = new File(cwd, ".git")

  val resolvedGitDir =
    if (gitFile.isFile)
      lines(gitFile.toPath)
        .iterator()
        .asScala
        .toList match {
        case gitDirLineRegex(dir) :: Nil ⇒
          log.info(s"Resolved git submodule file $gitFile to $dir")
          new File(dir)
        case lines ⇒
          throw new IllegalArgumentException(
            s"Expected single 'gitdir' line in .git file, found:\n\t${lines.mkString("\n\t")}"
          )
      }
    else
      gitFile

  val repository = FileRepositoryBuilder.create(resolvedGitDir)
  val storedConfig = repository.getConfig
  log.info("Repository = " + repository.getDirectory)

  def remotes: Seq[String] = {
    storedConfig.getSubsections("remote").asScala.to(Seq)
  }

  def remoteUrl(remoteName: String): String = {
    storedConfig.getString("remote", remoteName, "url")
  }

  def currentBranch: String = repository.getBranch

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
