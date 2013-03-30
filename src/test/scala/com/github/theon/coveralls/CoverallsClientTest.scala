package com.github.theon.coveralls

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import scalaj.http.Http.Request

/**
 * Date: 30/03/2013
 * Time: 15:02
 */
class CoverallsClientTest extends WordSpec with BeforeAndAfterAll with ShouldMatchers {

  def newTestHttpClient = new HttpClient {
    var dataIn:String = _

    def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]) = {
      dataIn = new String(data)
      """
        {
          "message":"test message",
          "error": false,
          "url": "https://github.com/theon/xsbt-coveralls-plugin"
        }
      """
    }
  }

  "CoverallsClient" when {
    "making API call" should {
      "return a valid response for success" in {
        val testHttpClient = newTestHttpClient
        val coverallsClient = new CoverallsClient {
          def httpClient = testHttpClient
        }

        val response = coverallsClient.postFile("src/test/resources/TestSourceFile.scala")

        testHttpClient.dataIn should equal("/**\n * Test Scala Source File that is 10 lines\n */\nclass TestSourceFile {\n\n\n\n\n\n}")
        response.message should equal("test message")
        response.error should equal(false)
        response.url should equal("https://github.com/theon/xsbt-coveralls-plugin")
      }
    }
  }
}
