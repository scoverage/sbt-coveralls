package org.scoverage.coveralls

import scala.io.{ Codec, Source }
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scalaj.http.{ HttpException, MultiPart, Http }
import scalaj.http.HttpOptions._
import java.io.File
import javax.net.ssl.{ SSLSocket, SSLSocketFactory }
import java.net.{ HttpURLConnection, Socket, InetAddress }
import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.databind.ObjectMapper

class CoverallsClient(endpoint: String, httpClient: HttpClient, sourcesEnc: Codec, jsonEnc: JsonEncoding) {

  import CoverallsClient._

  val mapper = newMapper
  def url: String = s"$endpoint/api/v1/jobs"

  def newMapper = {
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper
  }

  def postFile(file: File): CoverallsResponse = {
    val source = Source.fromFile(file)(sourcesEnc)
    // API want newlines encoded as \n, not sure about other escape chars
    // https://coveralls.zendesk.com/hc/en-us/articles/201774865-API-Introduction
    val bytes = source.getLines().mkString("\\n").getBytes(jsonEnc.getJavaName)
    source.close()

    httpClient.multipart(url, "json_file", "json_file.json", "application/json; charset=" + jsonEnc.getJavaName.toLowerCase, bytes) match {
      case CoverallHttpResponse(_, body) =>
        try {
          mapper.readValue(body, classOf[CoverallsResponse])
        } catch {
          case t: Throwable =>
            println("Failed to parse coveralls response: " + body)
            CoverallsResponse("Failed to parse response: " + t, error = true, "")
        }
    }
  }
}

object CoverallsClient {
  val tokenErrorString = "Couldn't find a repository matching this job"
  val errorResponseTitleTag = "title"
  val defaultErrorMessage = "ERROR (no title found)"
}

case class CoverallHttpResponse(responseCode: Int, body: String)

case class CoverallsResponse(message: String, error: Boolean, url: String)

trait HttpClient {
  def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]): CoverallHttpResponse
}

class ScalaJHttpClient extends HttpClient {

  val openJdkSafeSsl = new OpenJdkSafeSsl

  def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]): CoverallHttpResponse = try {
    val request = Http(url).postMulti(MultiPart(name, filename, mime, data))
      .option(connTimeout(60000))
      .option(readTimeout(60000))
      .option(sslSocketFactory(openJdkSafeSsl))

    val response = request.execute()
    CoverallHttpResponse(response.code, response.body)
  } catch {
    case e: HttpException => CoverallHttpResponse(500, e.message)
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
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
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
    "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
  ) intersect SSLSocketFactory.getDefault.asInstanceOf[SSLSocketFactory].getSupportedCipherSuites

  def getDefaultCipherSuites = Array.empty

  def getSupportedCipherSuites = Array.empty

  def createSocket(p1: Socket, p2: String, p3: Int, p4: Boolean) = safeSocket(child.createSocket(p1, p2, p3, p4))

  def createSocket(p1: String, p2: Int) = safeSocket(child.createSocket(p1, p2))

  def createSocket(p1: String, p2: Int, p3: InetAddress, p4: Int) = safeSocket(child.createSocket(p1, p2, p3, p4))

  def createSocket(p1: InetAddress, p2: Int) = safeSocket(child.createSocket(p1, p2))

  def createSocket(p1: InetAddress, p2: Int, p3: InetAddress, p4: Int) = safeSocket(child.createSocket(p1, p2, p3, p4))

  def safeSocket(sock: Socket) = sock match {
    case ssl: SSLSocket =>
      ssl.setEnabledCipherSuites(safeCiphers); ssl
    case other => other
  }
}
