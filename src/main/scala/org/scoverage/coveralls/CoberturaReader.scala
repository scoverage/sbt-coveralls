package org.scoverage.coveralls

import xml.Node
import scala.io.{ Codec, Source }
import java.io.File

class CoberturaReader(coberturaFile: File, childProjectRoot: File, rootProject: File, enc: Codec) {

  val elem: xml.Elem = XmlHelper.loadXmlFile(coberturaFile)

  val rootProjectDir = rootProject.getAbsolutePath + File.separator
  val childProjectDir = childProjectRoot.getAbsolutePath + File.separator

  def sourceFilenames = {
    (elem \\ "class" \\ "@filename").map { childProjectDir + _.toString }.toSet
  }

  /**
   * @return Map[Int,Int]
   */
  protected def lineCoverage(sourceFile: String) = {
    val classElems = elem \\ "class"
    val fileElems = classElems filter { n: Node => (childProjectDir + (n \\ "@filename").toString) == sourceFile }
    val lineElems = fileElems.flatMap(n => {
      n \\ "line"
    })

    lineElems.map(n => {
      (n \\ "@number").toString().toInt -> (n \\ "@hits").toString().toInt
    }).toMap
  }

  def reportForSource(source: String) = {
    val fileSrc = Source.fromFile(source)(enc)
    val lineCount = fileSrc.getLines().size
    fileSrc.close()

    val lineHitMap = lineCoverage(source)
    val fullLineHit = (0 until lineCount).map(i => lineHitMap.get(i + 1))

    SourceFileReport(rootProjectDir, source, fullLineHit.toList)
  }
}

case class CoberturaReaderException(cause: Exception) extends Exception(cause)

case class SourceFileReport(projectRoot: String, file: String, lineCoverage: List[Option[Int]]) {
  def fileRel = file.replace(projectRoot, "")
}
