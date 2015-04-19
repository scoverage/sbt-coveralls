package com.github.theon.coveralls

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.Matchers
import java.io.File
import scala.io.Codec
import org.scoverage.coveralls.CoberturaReader

class CoberturaReaderTest extends WordSpec with BeforeAndAfterAll with Matchers {

  val reader = new CoberturaReader(new File("src/test/resources/test_cobertura.xml"), new File(""), new File(""), Codec("UTF-8"))

  "CoberturaReader" when {
    "reading a Cobertura file" should {

      "List the correct source files" in {
        val sourceFiles = reader.sourceFilenames
        sourceFiles.exists(_.endsWith("src/test/resources/TestSourceFile.scala")) should equal(true)
        sourceFiles.exists(_.endsWith("src/test/resources/TestSourceFile2.scala")) should equal(true)
      }

      "return a valid SourceFileReport instance" in {
        val fileReport = reader.reportForSource(new File("").getAbsolutePath + File.separator + "src/test/resources/TestSourceFile.scala")
        fileReport.file should endWith("src/test/resources/TestSourceFile.scala")
        fileReport.projectRoot should equal(new File("").getAbsolutePath + File.separator)
        fileReport.lineCoverage should equal(
          List(None, None, None, Some(1), Some(1), Some(2), None, None, Some(1), Some(1))
        )
      }
    }
  }
}
