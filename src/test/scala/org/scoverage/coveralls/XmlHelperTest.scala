package org.scoverage.coveralls

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import scala.xml.XML

class XmlHelperTest extends AnyWordSpec with Matchers {

  val root = new File(getClass.getResource("/").getFile)

  val invalidDTD = new File(root, "test_cobertura_dtd.xml")

  "XmlHelper" when {

    "parsing XML documents with an invalid DTD" should {

      "not attempt to fetch the DTD and successfully parse" in {
        XmlHelper.loadXmlFile(invalidDTD) shouldBe an[xml.Elem]
        // Verify the document actually has an unusable DTD
        assertThrows[org.xml.sax.SAXParseException](XML.loadFile(invalidDTD))
      }
    }

  }
}
