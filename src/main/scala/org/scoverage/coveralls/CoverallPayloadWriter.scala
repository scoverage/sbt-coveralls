package org.scoverage.coveralls

import java.io.{File, FileInputStream}
import java.security.{DigestInputStream, MessageDigest}

import sbt.Logger
import com.fasterxml.jackson.core.{ JsonFactory, JsonEncoding }

class CoverallPayloadWriter(
    repoRootDir: File,
    coverallsFile: File,
    repoToken: Option[String],
    serviceName: Option[String],
    serviceNumber: Option[String],
    serviceJobId: Option[String],
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
    writeOpt("service_number", serviceNumber)
    writeOpt("service_job_id", serviceJobId)
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

    val sourceDigest = computeSourceDigest(report.file)

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

  private def computeSourceDigest(path: String) = {
    val buffer = new Array[Byte](8192)
    val md5 = MessageDigest.getInstance("MD5")

    val dis = new DigestInputStream(new FileInputStream(new File(path)), md5)
    try { while (dis.read(buffer) != -1) { } } finally { dis.close() }

    md5.digest.map("%02x".format(_)).mkString.toUpperCase
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
