package com.github.theon.coveralls

import java.io.File
import scala.io.{Codec, Source}

import sbt.Logger
import annotation.tailrec
import com.fasterxml.jackson.core.{JsonFactory, JsonEncoding}

class CoverallPayloadWriter(coverallsFile: File,
                            repoToken: Option[String],
                            travisJobId: Option[String],
                            serviceName: Option[String],
                            gitClient: GitClient,
                            sourcesEnc: Codec,
                            jsonEnc: JsonEncoding) {

  import gitClient._

  val gen = generator(coverallsFile)

  def generator(file: File) = {
    if(!file.getParentFile.exists) file.getParentFile.mkdirs
    val factory = new JsonFactory
    factory.createGenerator(file, jsonEnc)
  }

  def start(implicit log: Logger) {
    gen.writeStartObject

    def writeOpt(fieldName: String, holder: Option[String]) =
      holder foreach { gen.writeStringField(fieldName, _) }

    writeOpt("repo_token", repoToken)
    writeOpt("service_name", serviceName)
    writeOpt("service_job_id", travisJobId)

    addGitInfo

    gen.writeFieldName("source_files")
    gen.writeStartArray
  }

  private def addGitInfo(implicit log: Logger) {
    gen.writeFieldName("git")
    gen.writeStartObject

    gen.writeFieldName("head")
    gen.writeStartObject

    gen.writeStringField("id", lastCommit("%H"))
    gen.writeStringField("author_name", lastCommit("%an"))
    gen.writeStringField("author_email", lastCommit("%ae"))
    gen.writeStringField("committer_name", lastCommit("%cn"))
    gen.writeStringField("committer_email", lastCommit("%ce"))
    gen.writeStringField("message", lastCommit("%s"))

    gen.writeEndObject

    gen.writeStringField("branch", currentBranch)

    gen.writeFieldName("remotes")
    gen.writeStartArray

    addGitRemotes(remotes)

    gen.writeEndArray

    gen.writeEndObject
  }

  @tailrec
  private def addGitRemotes(remotes: Seq[String])(implicit log: Logger) {
    if(remotes.isEmpty) return

    gen.writeStartObject
    gen.writeStringField("name", remotes.head)
    gen.writeStringField("url", remoteUrl(remotes.head))
    gen.writeEndObject

    addGitRemotes(remotes.tail)
  }

  def addSourceFile(report: SourceFileReport) {
    gen.writeStartObject
    gen.writeStringField("name", report.fileRel)

    val source = Source.fromFile(report.file)(sourcesEnc)
    val sourceCode = source.mkString
    source.close

    gen.writeStringField("source", sourceCode)

    gen.writeFieldName("coverage")
    gen.writeStartArray
    report.lineCoverage.foreach {
      case Some(x) => gen.writeNumber(x)
      case _ => gen.writeNull()
    }
    gen.writeEndArray
    gen.writeEndObject
  }

  def end() {
    gen.writeEndArray
    gen.writeEndObject
    gen.flush
    gen.close
  }

  def flush {
    gen.flush
  }
}
