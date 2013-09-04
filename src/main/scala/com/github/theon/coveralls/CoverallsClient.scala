package com.github.theon.coveralls

import scala.io.{Codec, Source}
import org.codehaus.jackson.map.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scalaj.http.{HttpException, MultiPart, HttpOptions, Http}
import scalaj.http.HttpOptions._
import scalaj.http.Http.Request
import java.io.File
import org.codehaus.jackson.JsonEncoding
import javax.net.ssl.{SSLSocket, SSLSocketFactory}
import java.net.{Socket, InetAddress}

/**
 * Date: 10/03/2013
 * Time: 17:19
 */
class CoverallsClient(httpClient: HttpClient, sourcesEnc: Codec, jsonEnc: JsonEncoding) {

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
    val source = Source.fromFile(file)(sourcesEnc)
    val bytes = source.getLines.mkString("\n").getBytes(jsonEnc.getJavaName)
    source.close

    val res = httpClient.multipart(url, "json_file","json_file.json", "application/json; charset=" + jsonEnc.getJavaName.toLowerCase, bytes)
    mapper.readValue(res, classOf[CoverallsResponse])
  }
}

case class CoverallsResponse(message:String, error:Boolean, url:String)

trait HttpClient {
  def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]): String
}

class ScalaJHttpClient extends HttpClient {

  val openJdkSafeSsl = new OpenJdkSafeSsl

  def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]) = try {
    Http.multipart(url, MultiPart(name, filename, mime, data))
      .option(connTimeout(60000))
      .option(readTimeout(60000))
      .option(sslSocketFactory(openJdkSafeSsl))
      .asString
  } catch {
    case e:HttpException => e.body
  }
}

class OpenJdkSafeSsl extends SSLSocketFactory {
  val child = SSLSocketFactory.getDefault.asInstanceOf[SSLSocketFactory]

  val safeCiphers = Array(
    "SSL_RSA_WITH_RC4_128_MD5",
    "SSL_RSA_WITH_RC4_128_SHA",
    "TLS_RSA_WITH_AES_128_CBC_SHA",
    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
    "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
    "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
    "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
    "SSL_RSA_WITH_DES_CBC_SHA",
    "SSL_DHE_RSA_WITH_DES_CBC_SHA",
    "SSL_DHE_DSS_WITH_DES_CBC_SHA",
    "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
    "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
    "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
    "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
    "TLS_EMPTY_RENEGOTIATION_INFO_SCSV")

  def getDefaultCipherSuites = Array.empty
  def getSupportedCipherSuites = Array.empty

  def createSocket(p1: Socket, p2: String, p3: Int, p4: Boolean) = safeSocket(child.createSocket(p1, p2, p3, p4))
  def createSocket(p1: String, p2: Int) = safeSocket(child.createSocket(p1, p2))
  def createSocket(p1: String, p2: Int, p3: InetAddress, p4: Int) = safeSocket(child.createSocket(p1, p2, p3, p4))
  def createSocket(p1: InetAddress, p2: Int) = safeSocket(child.createSocket(p1, p2))
  def createSocket(p1: InetAddress, p2: Int, p3: InetAddress, p4: Int) = safeSocket(child.createSocket(p1, p2, p3, p4))

  def safeSocket(sock: Socket) = sock match {
    case ssl: SSLSocket => ssl.setEnabledCipherSuites(safeCiphers); ssl
    case other => other
  }
}