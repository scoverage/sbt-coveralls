package com.github.theon.coveralls

import java.io.File

import org.scalatest.{ Matchers, WordSpec }
import org.scoverage.coveralls.XmlHelper

import scala.xml.XML

class XmlHelperTest extends WordSpec with Matchers {

  val root = new File(getClass.getResource("/").getFile)

  val invalidDTD = new File(root, "test_cobertura_dtd.xml")

  "XmlHelper" when {

    "parsing XML documents with an invalid DTD" should {

      "not attempt to fetch the DTD and successfully parse" in {
        XmlHelper.loadXmlFile(invalidDTD) shouldBe an[xml.Elem]
        // Verify the document actually has an unusable DTD
        assertThrows[java.net.ConnectException](XML.loadFile(invalidDTD))
      }
    }

  }
}
