package org.scoverage.coveralls

import scala.io.Source
import scala.language.postfixOps
import java.io.File

class CoberturaMultiSourceReader(coberturaFile: File, sourceDirs: Seq[File], sourceEncoding: Option[String]) {

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
   *         It returns false if child and parent points to the same directory
   */
  def isChild(child: File, parent: File): Boolean = {
    val childPath = child.toPath
    val parentPath = parent.toPath
    childPath != parentPath && childPath.startsWith(parentPath)
  }

  val reportXML: xml.Elem = XmlHelper.loadXmlFile(coberturaFile)

  private val lineCoverageMap: Map[String, Map[Int, Int]] = {
    (reportXML \\ "class").foldLeft(Map[String, Map[Int, Int]]().empty) { (x, n) =>
      val fileName = (n \ "@filename").toString()
      val lineCoverage = (n \ "lines" \ "line").map(
        l => (l \ "@number").toString().toInt -> (l \ "@hits").toString().toInt
      )
      x + (fileName -> (x.getOrElse(fileName, Map.empty) ++ lineCoverage))
    }
  }

  /**
   * A sequence of source files paths that are relative to some source directory
   */
  private def sourceFilesRelative: Set[String] = lineCoverageMap.keySet

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
   *   2. the relative path to the  file
   * Note: that paths contains File.separator that is dependant on the system
   *
   * @return a tuple (a,b) such that "a/b" is the canonical path to sourceFile
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

    lineCoverageMap(filenamePath)
  }

  def reportForSource(source: String) = {
    val fileSrc = sourceEncoding match {
     case Some(enc) => Source.fromFile(source, enc)
     case None => Source.fromFile(source)
    }
    val lineCount = fileSrc.getLines().size
    fileSrc.close()

    val lineHitMap = lineCoverage(source)
    val fullLineHit = (0 until lineCount).map(i => lineHitMap.get(i + 1))

    val sourceNormalized = source.replace(File.separator, "/")

    SourceFileReport(sourceNormalized, fullLineHit.toList)
  }
}

case class SourceFileReport(file: String, lineCoverage: List[Option[Int]]) {
}
