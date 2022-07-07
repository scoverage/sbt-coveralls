package org.scoverage.coveralls

import java.io.File
import java.net.HttpURLConnection

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll

import scala.util.Try
import scalaj.http.Http
import scalaj.http.HttpOptions._

class CoverallsClientTest extends AnyWordSpec with BeforeAndAfterAll with Matchers {

  val projectDir = Utils.mkFileFromPath(Seq("."))
  val resourceDir = Utils.mkFileFromPath(projectDir, Seq("src", "test", "resources"))

  val defaultEndpoint = "https://coveralls.io"

  "CoverallsClient" when {
    "making API call" should {

      "return a valid response for success" in {
        val testHttpClient = new HttpClientTestSuccess()
        val coverallsClient = new CoverallsClient(defaultEndpoint, testHttpClient)

        val sourceFile = Utils.mkFileFromPath(resourceDir, Seq("projectA", "src", "main", "scala", "bar", "foo", "TestSourceFile.scala"))
        val response = coverallsClient.postFile(sourceFile)

        testHttpClient.dataIn should equal("""package bar.foo\n/**\n * Test Scala Source File that is 10 lines\n */\nclass TestSourceFile {\n\n\n\n\n}""")
        response.message should equal("test message")
        response.error should equal(false)
        response.url should equal("https://github.com/theon/xsbt-coveralls-plugin")
      }

      "return a valid response with Korean for success" in {
        val testHttpClient = new HttpClientTestSuccess()
        val coverallsClient = new CoverallsClient(defaultEndpoint, testHttpClient)

        val sourceFile = Utils.mkFileFromPath(resourceDir, Seq("projectA", "src", "main", "scala", "bar", "foo", "TestSourceFileWithKorean.scala"))
        val response = coverallsClient.postFile(sourceFile)

        testHttpClient.dataIn should equal("""/**\n * 한글 테스트\n */\nclass TestSourceFileWithKorean {\n\n\n\n\n\n}""")
        response.message should equal("test message")
        response.error should equal(false)
        response.url should equal("https://github.com/theon/xsbt-coveralls-plugin")
      }

      "work when there is no title in an error HTTP response" in {
        val testHttpClient = HttpClientTestFake(
          500,
          """{"message":"Couldn't find a repository matching this job.","error":true}"""
        )
        val coverallsClient = new CoverallsClient(defaultEndpoint, testHttpClient)

        val sourceFile = Utils.mkFileFromPath(resourceDir, Seq("projectA", "src", "main", "scala", "bar", "foo", "TestSourceFileWithKorean.scala"))
        val attemptAtResponse = Try {
          coverallsClient.postFile(sourceFile)
        }

        assert(attemptAtResponse.isSuccess)
        assert(attemptAtResponse.get.message == "Couldn't find a repository matching this job.")
        assert(attemptAtResponse.get.error)

      }

      "use the endpoint to build the url" in {
        val testHttpClient = new HttpClientTestSuccess()
        val coverallsClient = new CoverallsClient("https://test.endpoint", testHttpClient)

        assert(coverallsClient.url == "https://test.endpoint/api/v1/jobs")
      }
    }
  }

  "OpenJdkSafeSsl" when {
    val url = "https://coveralls.io/api/v1/jobs"
    "connecting to " + url should {
      "connect using ssl" in {
        val openJdkSafeSsl = new OpenJdkSafeSsl
        val request = Http(url)
          .method("GET")
          .option(connTimeout(60000))
          .option(readTimeout(60000))
          .option(sslSocketFactory(openJdkSafeSsl))

        val response = request.execute()
        response.code should equal(404)
      }
    }
  }
}
