package com.github.theon.coveralls

import java.io.File
import io.Source
import org.codehaus.jackson.{JsonEncoding, JsonFactory}
import org.codehaus.jackson.io.JsonStringEncoder

/**
 * Date: 10/03/2013
 * Time: 18:45
 */
trait CoverallPayloadWriter {

  def file:String
  def repoToken:String

  val gen = generator(file)
  val stringEncoder = new JsonStringEncoder()

  def generator(file:String) = {
    val factory = new JsonFactory()
    factory.createJsonGenerator(new File(file), JsonEncoding.UTF8);
  }

  def start() {
    gen.writeStartObject
    gen.writeStringField("repo_token", repoToken)

    gen.writeRaw(
      """
        ,
        "git": {
          "head": {
            "id": "",
            "author_name": "Ian Forsey",
            "author_email": "...",
            "committer_name": "Ian Forsey",
            "committer_email": "",
            "message": ""
          },
          "branch": "master",
          "remotes": [
            {
              "name": "origin",
              "url": "git@github.com:theon/scala-uri.git"
            }
          ]
        }
      """)

    gen.writeFieldName("source_files")
    gen.writeStartArray
  }

  def addSouceFile(report:SourceFileReport) {
    gen.writeStartObject
    gen.writeStringField("name", report.file)

    val source = Source.fromFile(report.file).mkString
    gen.writeStringField("source", source)

    gen.writeFieldName("coverage")
    gen.writeStartArray
    report.lineCoverage.foreach(hit => {
      hit match {
        case Some(x) => gen.writeNumber(x)
        case _ => gen.writeNull()
      }
    })
    gen.writeEndArray
    gen.writeEndObject
  }

  def end() {
    gen.writeEndArray
    gen.writeEndObject
    gen.flush()
  }
}
