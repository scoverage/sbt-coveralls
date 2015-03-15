package com.github.theon.coveralls

import java.io.File

import com.fasterxml.jackson.core.JsonEncoding
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import org.scoverage.coveralls
import org.scoverage.coveralls.CoverallsClient

import scala.io.Codec
import scala.util.{Success, Try}

class CoverallsClientTest extends WordSpec with BeforeAndAfterAll with Matchers {

  "CoverallsClient" when {
    "making API call" should {

      "return a valid response for success" in {
        val testHttpClient = new TestSuccessHttpClient()
        val coverallsClient = new CoverallsClient(testHttpClient, Codec.UTF8, JsonEncoding.UTF8)

        val response = coverallsClient.postFile(new File("src/test/resources/TestSourceFile.scala"))

        testHttpClient.dataIn should equal("""/**\n * Test Scala Source File that is 10 lines\n */\nclass TestSourceFile {\n\n\n\n\n\n}""")
        response.message should equal("test message")
        response.error should equal(false)
        response.url should equal("https://github.com/theon/xsbt-coveralls-plugin")
      }

      "return a valid response with Korean for success" in {
        val testHttpClient = new TestSuccessHttpClient()
        val coverallsClient = new CoverallsClient(testHttpClient, Codec.UTF8, JsonEncoding.UTF8)

        val response = coverallsClient.postFile(new File("src/test/resources/TestSourceFileWithKorean.scala"))

        testHttpClient.dataIn should equal("""/**\n * 한글 테스트\n */\nclass TestSourceFileWithKorean {\n\n\n\n\n\n}""")
        response.message should equal("test message")
        response.error should equal(false)
        response.url should equal("https://github.com/theon/xsbt-coveralls-plugin")
      }

      "work when there is no title in an error HTTP response" in {
        val testHttpClient = FakeTestHttpClient(500,
          """
          <html>
            <body>
              This is a test error returned html document.
            </body>
          </html>
          """
        )
        val coverallsClient = new CoverallsClient(testHttpClient, Codec.UTF8, JsonEncoding.UTF8)

        val attemptAtResponse = Try {
          coverallsClient.postFile(new File("src/test/resources/TestSourceFileWithKorean.scala"))
        }

        assert(attemptAtResponse.isSuccess)
        assert(attemptAtResponse.get.message == CoverallsClient.defaultErrorMessage)
        assert(attemptAtResponse.get.error)

      }

      "work when there is a title in an error HTTP response" in {
        val testErrorMessage = "test error message when there was an error"
        val testHttpClient = FakeTestHttpClient(
          500,
          s"""
          <html>
            <title>
             $testErrorMessage
            </title>
            <body>
              This is a test error returned html document.
            </body>
          </html>
          """
        )
        val coverallsClient = new CoverallsClient(testHttpClient, Codec.UTF8, JsonEncoding.UTF8)

        val attemptAtResponse = Try {
          coverallsClient.postFile(new File("src/test/resources/TestSourceFileWithKorean.scala"))
        }

        assert(attemptAtResponse.isSuccess)
        assert(attemptAtResponse.get.message == testErrorMessage)
        assert(attemptAtResponse.get.error)
      }
    }
  }
}
