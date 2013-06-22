package com.github.theon.coveralls

import xml.{Node, XML}
import scala.io.{Codec, Source}
import java.io.File

/**
 * Date: 10/03/2013
 * Time: 17:42
 */
class CoberturaReader(val file: File, val baseDirFile: File, enc: Codec) {

  val elem = XML.loadFile(file)

  val baseDir = baseDirFile.getAbsolutePath + File.separator

  def sourceFilenames = {
    elem \\ "class" \\ "@filename"  map { baseDir + _.toString } toSet
  }

  /**
   * @return Map[Int,Int]
   */
  protected def lineCoverage(sourceFile: String) = {
    val classElems = (elem \\ "class")
    val fileElems = classElems filter { n:Node => (baseDir + (n \\ "@filename").toString) == sourceFile }
    val lineElems = fileElems.flatMap(n => {
      n \\ "line"
    })

    lineElems.map(n => {
      (n \\ "@number").toString.toInt -> (n \\ "@hits").toString.toInt
    }).toMap
  }

  def reportForSource(source: String) = {
    val fileSrc = Source.fromFile(source)(enc)
    val lineCount = fileSrc.getLines().size
    fileSrc.close

    val lineHitMap = lineCoverage(source)
    val fullLineHit = (0 until lineCount).map(i => lineHitMap.get(i + 1))

    SourceFileReport(source, fullLineHit.toList)
  }
}

case class CoberturaReaderException(cause: Exception) extends Exception(cause)

case class SourceFileReport(file: String, lineCoverage: List[Option[Int]])
