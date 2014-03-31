package com.github.theon.coveralls

import sbt.Logger
import scala.sys.process._
import java.io.File

class GitClient {

  def cwd = Option.empty[String]

  def remotes(implicit log: Logger) =
    execute("git remote")

  def remoteUrl(remoteName: String)(implicit log: Logger) = {
    execute("git config --get remote." + remoteName + ".url").head
  }

  def currentBranch(implicit log: Logger) =
    execute("git rev-parse --abbrev-ref HEAD").head

  def lastCommit(format: String)(implicit log: Logger) =
    execute("git log -n1 --pretty=format:" + format).head

  protected def execute(cmd: String)(implicit log: Logger): Seq[String] = {
    val process = Process(cmd, cwd.map(new File(_)))
    log.debug("About to execute process '%s'." format process)
    var (out, err) = (Vector[String](), Vector[String]())
    val exitCode = process ! ProcessLogger(out :+= _, err :+= _)
    if (exitCode == 0)
      out
    else {
      sys.error("Exit code: %s\n%s".format(exitCode, err mkString "\n"))
    }
  }
}
