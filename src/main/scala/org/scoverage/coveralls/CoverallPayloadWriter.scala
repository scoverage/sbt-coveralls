package org.scoverage.coveralls

import java.io.File
import java.security.MessageDigest
import scala.io.Source

import sbt.Logger
import com.fasterxml.jackson.core.{ JsonFactory, JsonEncoding }

class CoverallPayloadWriter(
    repoRootDir: File,
    coverallsFile: File,
    repoToken: Option[String],
    jobId: Option[String],
    serviceName: Option[String],
    pullRequest: Option[String],
    parallel: Boolean,
    sourceEncoding: Option[String],
    gitClient: GitClient) {

  val repoRootDirStr = repoRootDir.getCanonicalPath.replace(File.separator, "/") + "/"
  import gitClient._

  val gen = generator(coverallsFile)

  def generator(file: File) = {
    if (!file.getParentFile.exists) file.getParentFile.mkdirs
    val factory = new JsonFactory
    factory.createGenerator(file, JsonEncoding.UTF8)
  }

  def start() {
    gen.writeStartObject()

    def writeOpt(fieldName: String, holder: Option[String]) =
      holder foreach { gen.writeStringField(fieldName, _) }

    writeOpt("repo_token", repoToken)
    writeOpt("service_name", serviceName)
    writeOpt("service_job_id", jobId)
    writeOpt("service_pull_request", pullRequest)

    gen.writeBooleanField("parallel", parallel)

    addGitInfo

    gen.writeFieldName("source_files")
    gen.writeStartArray()
  }

  private def addGitInfo() {
    gen.writeFieldName("git")
    gen.writeStartObject()

    gen.writeFieldName("head")
    gen.writeStartObject()

    val commitInfo = lastCommit()

    gen.writeStringField("id", commitInfo.id)
    gen.writeStringField("author_name", commitInfo.authorName)
    gen.writeStringField("author_email", commitInfo.authorEmail)
    gen.writeStringField("committer_name", commitInfo.committerName)
    gen.writeStringField("committer_email", commitInfo.committerEmail)
    gen.writeStringField("message", commitInfo.shortMessage)

    gen.writeEndObject()

    gen.writeStringField("branch", currentBranch)

    gen.writeFieldName("remotes")
    gen.writeStartArray()

    addGitRemotes(remotes)

    gen.writeEndArray()

    gen.writeEndObject()
  }

  private def addGitRemotes(remotes: Seq[String]) {
    remotes.foreach( remote => {
      gen.writeStartObject()
      gen.writeStringField("name", remote)
      gen.writeStringField("url", remoteUrl(remote))
      gen.writeEndObject()
    })
  }

  def addSourceFile(report: SourceFileReport) {

    // create a name relative to the project root (rather than the module root)
    // this is needed so that coveralls can find the file in git.
    val fileName = report.file.replace(repoRootDirStr, "")

    gen.writeStartObject()
    gen.writeStringField("name", fileName)

    val source = sourceEncoding match {
     case Some(enc) => Source.fromFile(report.file, enc)
     case None => Source.fromFile(report.file)
    }
    val sourceCode = source.getLines().mkString("\n")
    source.close()

    val sourceDigest = CoverallPayloadWriter.md5.digest(sourceCode.getBytes).map("%02X" format _).mkString

    gen.writeStringField("source_digest", sourceDigest)

    gen.writeFieldName("coverage")
    gen.writeStartArray()
    report.lineCoverage.foreach {
      case Some(x) => gen.writeNumber(x)
      case _ => gen.writeNull()
    }
    gen.writeEndArray()
    gen.writeEndObject()
  }

  def end(): Unit = {
    gen.writeEndArray()
    gen.writeEndObject()
    gen.flush()
    gen.close()
  }

  def flush(): Unit = {
    gen.flush()
  }
}

object CoverallPayloadWriter {

  val md5: MessageDigest = MessageDigest.getInstance("MD5")

}
