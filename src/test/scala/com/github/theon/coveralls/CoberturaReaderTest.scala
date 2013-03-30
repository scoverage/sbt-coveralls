package com.github.theon.coveralls

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.ShouldMatchers

/**
 * Date: 30/03/2013
 * Time: 13:22
 */
class CoberturaReaderTest extends WordSpec with BeforeAndAfterAll with ShouldMatchers {

  val reader = new CoberturaReader {
    def file = "src/test/resources/test_cobertura.xml"
  }

  "CoberturaReader" when {
    "reading a Cobertura file" should {

      "List the correct source files" in {
        val sourceFiles = reader.sourceFilenames()
        sourceFiles should contain ("TestSourceFile.scala")
        sourceFiles should contain ("TestSourceFile2.scala")
      }

      "return a valid SourceFileReport instance" in {
        val fileReport = reader.reportForSource("src/test/resources", "TestSourceFile.scala")
        fileReport.file should equal("TestSourceFile.scala")
        fileReport.lineCoverage should equal(
          List(None, None, None, Some(1), Some(1), Some(2), None, None, Some(1), Some(1))
        )
      }
    }
  }
}
