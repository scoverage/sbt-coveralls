package com.github.theon.coveralls

import sbt.State
import scala.sys.process._

/**
 * Date: 29/03/2013
 * Time: 11:53
 */
object GitClient {

  def remotes(implicit state: State) =
    execute("git remote", state)

  def remoteUrl(remoteName:String)(implicit state: State) = {
    execute("git config --get remote." + remoteName + ".url", state).head
  }

  def currentBranch(implicit state: State) =
    execute("git rev-parse --abbrev-ref HEAD", state).head

  def lastCommit(format:String)(implicit state: State) =
    execute("git log -n1 --pretty=format:" + format, state).head

  protected def execute(process: ProcessBuilder, state: State): Seq[String] = {
    state.log.debug("About to execute process '%s'." format process)
    var (out, err) = (Vector[String](), Vector[String]())
    val exitCode = process ! ProcessLogger(out :+= _, err :+= _)
    if (exitCode == 0)
      out
    else {
      sys.error("Exit code: %s\n%s".format(exitCode, err mkString "\n"))
    }
  }
}
