package com.github.theon.coveralls

import io.Source
import org.codehaus.jackson.map.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scalaj.http.{HttpOptions, MultiPart, Http}
import scalaj.http.HttpOptions._

/**
 * Date: 10/03/2013
 * Time: 17:19
 */
trait CoverallsClient {

  val url = "https://coveralls.io/api/v1/jobs"
  val mapper = newMapper

  def newMapper = {
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper
  }

  /**
   * TODO: Performance improvement - don't read the whole file into memory - stream it from disk
   */
  def postFile(file:String) = {
    val source = Source.fromFile(file)
    val bytes = source.map(_.toByte).toArray
    source.close()

    val res = Http.multipart(url, MultiPart("json_file","json_file.json", "application/json", bytes))
      .option(connTimeout(5000), readTimeout(5000))
    mapper.readValue(res.asString, classOf[CoverallsResponse])
  }
}

case class CoverallsResponse(message:String, error:Boolean, url:String)
