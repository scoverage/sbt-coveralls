package com.github.theon.coveralls

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import scala.io.Codec
import java.io.File
import com.fasterxml.jackson.core.JsonEncoding
import org.scoverage.coveralls.CoverallsClient

/**
 * Date: 30/03/2013
 * Time: 15:02
 */
class CoverallsClientTest extends WordSpec with BeforeAndAfterAll with ShouldMatchers {

  "CoverallsClient" when {
    "making API call" should {
      "return a valid response for success" in {
        val testHttpClient = new TestSuccessHttpClient()
        val coverallsClient = new CoverallsClient(testHttpClient, Codec(Codec.UTF8), JsonEncoding.UTF8)

        val response = coverallsClient.postFile(new File("src/test/resources/TestSourceFile.scala"))

        testHttpClient.dataIn should equal("/**\n * Test Scala Source File that is 10 lines\n */\nclass TestSourceFile {\n\n\n\n\n\n}")
        response.message should equal("test message")
        response.error should equal(false)
        response.url should equal("https://github.com/theon/xsbt-coveralls-plugin")
      }
      "return a valid response with Korean for success" in {
        val testHttpClient = new TestSuccessHttpClient()
        val coverallsClient = new CoverallsClient(testHttpClient, Codec(Codec.UTF8), JsonEncoding.UTF8)

        val response = coverallsClient.postFile(new File("src/test/resources/TestSourceFileWithKorean.scala"))

        testHttpClient.dataIn should equal("/**\n * 한글 테스트\n */\nclass TestSourceFile {\n\n\n\n\n\n}")
        response.message should equal("test message")
        response.error should equal(false)
        response.url should equal("https://github.com/theon/xsbt-coveralls-plugin")
      }
    }
  }
}
