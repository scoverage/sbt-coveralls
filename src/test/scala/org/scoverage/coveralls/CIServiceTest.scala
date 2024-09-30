package org.scoverage.coveralls

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import sbt.ConsoleLogger

import scala.Option

class CIServiceTest extends AnyWordSpec with Matchers {

  "CIService" when {

    "getFromJson" should {
      "return a valid response" in {
        val lines = """
          |{
          | "textField": "textContent",
          | "numericField": 123,
          | "booleanField": true,
          | "nestedObject": {
          |   "arrayField": [1, 2, 3]
          | }
          |}
          |""".stripMargin

        GitHubActions.getFromJson(lines, "numericField") shouldBe Some("123")
      }
    }

    "getPrNumber" should {
      "return a valid response" in {
        val payloadPath = "src/test/resources/example-pr-response.json"
        GitHubActions.getPrNumber(payloadPath) shouldBe Some("1347")
      }
    }
  }

}
