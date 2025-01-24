package org.scoverage.coveralls

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scalaj.http.HttpOptions._
import scalaj.http.{Http, MultiPart}

import java.io.File
import java.net.{InetAddress, Socket}
import javax.net.ssl.{SSLSocket, SSLSocketFactory}
import scala.io.{BufferedSource, Codec, Source}
import scala.util.control.NonFatal

class CoverallsClient(endpoint: String, httpClient: HttpClient) {

  val mapper: ObjectMapper = newMapper
  def url: String = s"$endpoint/api/v1/jobs"

  def newMapper: ObjectMapper = {
    val mapper = new ObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper
  }

  def postFile(file: File): CoverallsResponse = {
    val codec: Codec = Codec.UTF8
    var source: BufferedSource = null
    try {
      source = Source.fromFile(file)(codec)
      // API want newlines encoded as \n, not sure about other escape chars
      // https://coveralls.zendesk.com/hc/en-us/articles/201774865-API-Introduction
      val bytes = source.getLines().mkString("\\n").getBytes(codec.charSet)

      httpClient.multipart(
        url,
        "json_file",
        "json_file.json",
        "application/json; charset=utf-8",
        bytes
      ) match {
        case CoverallHttpResponse(_, body) =>
          try {
            mapper.readValue(body, classOf[CoverallsResponse])
          } catch {
            case NonFatal(e) =>
              CoverallsResponse(
                "Failed to parse response: " + e,
                error = true,
                ""
              )
          }
      }
    } catch {
      case NonFatal(e) =>
        throw e
    } finally {
      if (source != null)
        source.close()
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
  def multipart(
      url: String,
      name: String,
      filename: String,
      mime: String,
      data: Array[Byte]
  ): CoverallHttpResponse
}

class ScalaJHttpClient extends HttpClient {

  val openJdkSafeSsl = new OpenJdkSafeSsl

  def multipart(
      url: String,
      name: String,
      filename: String,
      mime: String,
      data: Array[Byte]
  ): CoverallHttpResponse = try {
    val request = Http(url)
      .postMulti(MultiPart(name, filename, mime, data))
      .option(connTimeout(60000))
      .option(readTimeout(60000))
      .option(sslSocketFactory(openJdkSafeSsl))

    val response = request.execute()
    CoverallHttpResponse(response.code, response.body)
  } catch {
    case e: Exception => CoverallHttpResponse(500, e.getMessage)
  }
}

class OpenJdkSafeSsl extends SSLSocketFactory {
  val child: SSLSocketFactory =
    SSLSocketFactory.getDefault.asInstanceOf[SSLSocketFactory]

  val safeCiphers: Array[String] = Array(
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
  ) intersect SSLSocketFactory.getDefault
    .asInstanceOf[SSLSocketFactory]
    .getSupportedCipherSuites

  def getDefaultCipherSuites: Array[String] = Array.empty

  def getSupportedCipherSuites: Array[String] = Array.empty

  def createSocket(p1: Socket, p2: String, p3: Int, p4: Boolean): Socket =
    safeSocket(
      child.createSocket(p1, p2, p3, p4)
    )

  def createSocket(p1: String, p2: Int): Socket = safeSocket(
    child.createSocket(p1, p2)
  )

  def createSocket(p1: String, p2: Int, p3: InetAddress, p4: Int): Socket =
    safeSocket(
      child.createSocket(p1, p2, p3, p4)
    )

  def createSocket(p1: InetAddress, p2: Int): Socket = safeSocket(
    child.createSocket(p1, p2)
  )

  def createSocket(p1: InetAddress, p2: Int, p3: InetAddress, p4: Int): Socket =
    safeSocket(child.createSocket(p1, p2, p3, p4))

  def safeSocket(sock: Socket): Socket = sock match {
    case ssl: SSLSocket =>
      ssl.setEnabledCipherSuites(safeCiphers); ssl
    case other => other
  }
}
