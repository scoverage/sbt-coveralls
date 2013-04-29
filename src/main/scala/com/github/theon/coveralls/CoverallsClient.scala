package com.github.theon.coveralls

import io.Source
import org.codehaus.jackson.map.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scalaj.http.{HttpException, MultiPart, HttpOptions, Http}
import scalaj.http.HttpOptions._
import scalaj.http.Http.Request

/**
 * Date: 10/03/2013
 * Time: 17:19
 */
trait CoverallsClient {

  def httpClient:HttpClient
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

    val res = httpClient.multipart(url, "json_file","json_file.json", "application/json", bytes)
    mapper.readValue(res, classOf[CoverallsResponse])
  }
}

case class CoverallsResponse(message:String, error:Boolean, url:String)

trait HttpClient {
  def multipart(url:String, name:String, filename:String, mime:String, data:Array[Byte]): String
}

class ScalaJHttpClient extends HttpClient {
  def multipart(url:String, name:String, filename:String, mime:String, data:Array[Byte]) = try {
    Http.multipart(url, MultiPart(name, filename, mime, data))
      .option(connTimeout(60000)).option(readTimeout(60000))
      .asString
  } catch {
    case e:HttpException => e.body
  }
}