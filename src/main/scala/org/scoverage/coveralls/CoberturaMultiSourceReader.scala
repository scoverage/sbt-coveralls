package org.scoverage.coveralls

import xml.{ Node, XML }
import scala.io.{ Codec, Source }
import java.io.File
import sbt.IO
/**
 * The file will replace the original CoberturaReader
 */
class CoberturaMultiSourceReader(coberturaFile: File, sourceDirs: Seq[File], enc: Codec) {

  require(sourceDirs.nonEmpty, "Given empty sequence of source directories")

  {
    //there must not be nested source directories
    val relatedDirs = sourceDirs.combinations(2) find { case Seq(a, b) => isChild(a, b) }
    require(relatedDirs.isEmpty, "Source directories must not be nested: " +
      s"${relatedDirs.get(0).getCanonicalPath} is contained in ${relatedDirs.get(1).getCanonicalPath}")
  }

  /**
   * Checks whether a file belongs to another directory, recursively
   * @param child - a child file or directory
   * @param parent - a parent directory
   * @return false if parent is not a directory or if child does not belongs to
   *         the file tree rooted at parent.
   *         It returns true if child and parent points to the same directory
   */
  def isChild(child: File, parent: File): Boolean = IO.relativize(parent, child).isDefined

  val reportXML = XML.loadFile(coberturaFile)

  /**
   * A sequence of source files paths that are relative to some source directory
   */
  private def sourceFilesRelative: Set[String] = reportXML \\ "class" \\ "@filename" map (_.toString()) toSet

  def sourceFiles: Set[File] = {
    for {
      relativePath <- sourceFilesRelative
      sourceDir <- sourceDirs
      //only one directory contains the file
      sourceFile = new File(sourceDir, relativePath)
      if sourceFile.exists
    } yield sourceFile
  }

  def sourceFilenames = sourceFiles.map(_.getCanonicalPath)

  /**
   * Splits a path to a source file into two parts:
   *   1. the absolute path to source directory that contain this sourceFile
   *   2. the realtive path to the  file
   * Note: that paths contains File.separator that is dependant on the system
   *
   * @return a tuple (a,b) such that "a/b" is tha canonical path to sourceFile
   * @throws IllegalArgumentException when a given file does not belongs to any of
   *                                  the source directories
   */
  def splitPath(sourceFile: File): (String, String) = {
    val parentDir = sourceDirs find (p => isChild(sourceFile, p))
    require(parentDir.isDefined, s"The file ${sourceFile.getCanonicalPath} does not belong to any of" +
      s" the source directories ${sourceDirs.map(_.getCanonicalPath).mkString(",")}")

    val prefix = parentDir.get.getCanonicalPath
    val relativePath = sourceFile.getCanonicalPath.substring(prefix.length + 1)
    (prefix, relativePath)
  }

  protected def lineCoverage(sourceFile: String) = {
    val filenamePath = splitPath(new File(sourceFile))._2.replace(File.separator, "/")

    val classElems = reportXML \\ "class"
    val fileElems = classElems filter { n: Node => (n \\ "@filename").toString == filenamePath }
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

    val rootProjectDir = splitPath(new File(source))._1.replace(File.separator, "/") + "/"
    val sourceNoramlized = source.replace(File.separator, "/")

    SourceFileReport(rootProjectDir, sourceNoramlized, fullLineHit.toList)
  }
}
