package com.github.theon.coveralls

import scala.io.{Codec, Source}
import org.codehaus.jackson.map.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scalaj.http.{HttpException, MultiPart, HttpOptions, Http}
import scalaj.http.HttpOptions._
import scalaj.http.Http.Request
import java.io.File

/**
 * Date: 10/03/2013
 * Time: 17:19
 */
class CoverallsClient(httpClient: HttpClient, enc: Codec) {

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
  def postFile(file: File) = {
    val source = Source.fromFile(file)(enc)
    val bytes = source.getLines.mkString("\n").getBytes(enc.charSet)
    source.close()

    val res = httpClient.multipart(url, "json_file","json_file.json", "application/json; charset=UTF-8", bytes)
    mapper.readValue(res, classOf[CoverallsResponse])
  }
}

case class CoverallsResponse(message:String, error:Boolean, url:String)

trait HttpClient {
  def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]): String
}

class ScalaJHttpClient extends HttpClient {
  def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]) = try {
    Http.multipart(url, MultiPart(name, filename, mime, data))
      .option(connTimeout(60000)).option(readTimeout(60000))
      .asString
  } catch {
    case e:HttpException => e.body
  }
}
