package com.github.theon.coveralls

import xml.{Node, XML}
import io.Source

/**
 * Date: 10/03/2013
 * Time: 17:42
 */
trait CoberturaReader {

  def file:String
  val elem = XML.loadFile(file)

  def sourceFilenames() = {
    elem \\ "class" \\ "@filename"  map { _.toString } toSet
  }

  /**
   * @return Map[String,]
   */
  protected def lineCoverage(sourceFile:String) = {
    val classElems = (elem \\ "class")
    val fileElems = classElems filter { n:Node =>  (n \\ "@filename").toString == sourceFile }
    val lineElems = fileElems.flatMap(n => {
      n \\ "line"
    })

    lineElems.map(n => {
      (n \\ "@number").toString.toInt -> (n \\ "@hits").toString.toInt
    }).toMap
  }

  def reportForSource(baseDir:String, source:String) = {
    val lineCount = Source.fromFile(baseDir + "/" + source).getLines().size
    val lineHitMap:Map[Int,Int] = lineCoverage(source)
    val fullLineHit = (0 until lineCount).map(i => lineHitMap.get(i+1))

    SourceFileReport(source, fullLineHit.toList)
  }
}

case class SourceFileReport(file:String, lineCoverage:List[Option[Int]])
