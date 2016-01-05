package com.github.theon.coveralls

import java.io.File
import java.net.HttpURLConnection

import com.fasterxml.jackson.core.JsonEncoding
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import org.scoverage.coveralls.{ OpenJdkSafeSsl, CoverallsClient }

import scala.io.Codec
import scala.util.Try
import scalaj.http.Http
import scalaj.http.HttpOptions._

class CoverallsClientTest extends WordSpec with BeforeAndAfterAll with Matchers {

  val defaultEndpoint = "https://coveralls.io"

  "CoverallsClient" when {
    "making API call" should {

      "return a valid response for success" in {
        val testHttpClient = new TestSuccessHttpClient()
        val coverallsClient = new CoverallsClient(defaultEndpoint, testHttpClient, Codec.UTF8, JsonEncoding.UTF8)

        val response = coverallsClient.postFile(new File("src/test/resources/TestSourceFile.scala"))

        testHttpClient.dataIn should equal("""/**\n * Test Scala Source File that is 10 lines\n */\nclass TestSourceFile {\n\n\n\n\n\n}""")
        response.message should equal("test message")
        response.error should equal(false)
        response.url should equal("https://github.com/theon/xsbt-coveralls-plugin")
      }

      "return a valid response with Korean for success" in {
        val testHttpClient = new TestSuccessHttpClient()
        val coverallsClient = new CoverallsClient(defaultEndpoint, testHttpClient, Codec.UTF8, JsonEncoding.UTF8)

        val response = coverallsClient.postFile(new File("src/test/resources/TestSourceFileWithKorean.scala"))

        testHttpClient.dataIn should equal("""/**\n * 한글 테스트\n */\nclass TestSourceFileWithKorean {\n\n\n\n\n\n}""")
        response.message should equal("test message")
        response.error should equal(false)
        response.url should equal("https://github.com/theon/xsbt-coveralls-plugin")
      }

      "work when there is no title in an error HTTP response" in {
        val testHttpClient = FakeTestHttpClient(
          500,
          """{"message":"Couldn't find a repository matching this job.","error":true}"""
        )
        val coverallsClient = new CoverallsClient(defaultEndpoint, testHttpClient, Codec.UTF8, JsonEncoding.UTF8)

        val attemptAtResponse = Try {
          coverallsClient.postFile(new File("src/test/resources/TestSourceFileWithKorean.scala"))
        }

        assert(attemptAtResponse.isSuccess)
        assert(attemptAtResponse.get.message == "Couldn't find a repository matching this job.")
        assert(attemptAtResponse.get.error)

      }

      "use the endpoint to build the url" in {
        val testHttpClient = new TestSuccessHttpClient()
        val coverallsClient = new CoverallsClient("https://test.endpoint", testHttpClient, Codec.UTF8, JsonEncoding.UTF8)
        
        assert(coverallsClient.url == "https://test.endpoint/api/v1/jobs")
      }
    }
  }

  "OpenJdkSafeSsl" when {
    val url = "https://coveralls.io/api/v1/jobs"
    "connecting to " + url should {
      "connect using ssl" in {
        val openJdkSafeSsl = new OpenJdkSafeSsl
        val request = Http.get(url)
          .option(connTimeout(60000))
          .option(readTimeout(60000))
          .option(sslSocketFactory(openJdkSafeSsl))

        request.process { conn: HttpURLConnection =>
          conn.getResponseCode should equal(404)
        }
      }
    }
  }
}
