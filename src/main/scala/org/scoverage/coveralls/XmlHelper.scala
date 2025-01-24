package org.scoverage.coveralls

import org.xml.sax.InputSource

import java.io.{File, FileInputStream}
import javax.xml.parsers.SAXParserFactory
import scala.util.Try
import scala.xml.XML

/** A simple utility around XML.loadXml that doesn't depend on external DTD
  * fetching and processing. This avoids random failures when coburtura.xml DTD
  * clauses point to dead domains
  */
object XmlHelper {

  private[this] val factory: SAXParserFactory = locally {
    val f = SAXParserFactory.newInstance()
    f.setValidating(false)
    f.setFeature("http://xml.org/sax/features/validation", false)
    f.setFeature(
      "http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
      false
    )
    f.setFeature(
      "http://apache.org/xml/features/nonvalidating/load-external-dtd",
      false
    )
    f
  }

  def loadXmlFile(file: File): xml.Elem = {
    val parser = factory.newSAXParser()
    val stream = new FileInputStream(file)
    try {
      XML.loadXML(new InputSource(stream), parser)
    } finally {
      Try(stream.close())
    }
  }

}
