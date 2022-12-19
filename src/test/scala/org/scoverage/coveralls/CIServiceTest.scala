package org.scoverage.coveralls

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import sbt.ConsoleLogger

import scala.Option

class CIServiceTest extends AnyWordSpec with Matchers {

  implicit val log = ConsoleLogger(System.out)

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

        GitHubActions.getFromJson(lines, "numericField") === Some("123.0")    
      }
    }
  }
}
