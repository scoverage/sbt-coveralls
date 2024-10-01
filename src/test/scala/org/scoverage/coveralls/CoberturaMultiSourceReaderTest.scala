package org.scoverage.coveralls

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sbt.util.AbstractLogger

import java.io.{File, FileNotFoundException}

class CoberturaMultiSourceReaderTest
    extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers {
  implicit val log: AbstractLogger = sbt.Logger.Null

  val resourceDir = Utils.mkFileFromPath(Seq(".", "src", "test", "resources"))
  val sourceDirA =
    Utils.mkFileFromPath(resourceDir, Seq("projectA", "src", "main", "scala"))
  val sourceDirA212 =
    Utils.mkFileFromPath(
      resourceDir,
      Seq("projectA", "src", "main", "scala-2.12")
    )
  val sourceDirB =
    Utils.mkFileFromPath(resourceDir, Seq("projectB", "src", "main", "scala"))
  val sourceDirs = Seq(sourceDirA, sourceDirA212, sourceDirB)

  val reader = new CoberturaMultiSourceReader(
    Utils.mkFileFromPath(resourceDir, Seq("test_cobertura_multisource.xml")),
    sourceDirs,
    Some("UTF-8")
  )

  "CoberturaMultiSourceReader" when {
    "reading a Cobertura file" should {

      "list the correct source files" in {
        val sourceFiles = Seq(
          Utils.mkFileFromPath(
            sourceDirA,
            Seq("bar", "foo", "TestSourceFile.scala")
          ),
          Utils.mkFileFromPath(
            sourceDirA212,
            Seq("bar", "foo", "TestSourceScala212.scala")
          ),
          Utils.mkFileFromPath(sourceDirB, Seq("foo", "TestSourceFile.scala"))
        )
        reader.sourceFiles shouldEqual sourceFiles.toSet
      }

      "return a valid SourceFileReport instance" in {
        val sourceFile = Utils.mkFileFromPath(
          sourceDirA,
          Seq("bar", "foo", "TestSourceFile.scala")
        )
        val fileReport = reader.reportForSource(sourceFile.getCanonicalPath)
        fileReport.file should endWith(
          Seq("foo", "TestSourceFile.scala").mkString(File.separator)
        )
        fileReport.lineCoverage should equal(
          List(
            None,
            None,
            None,
            Some(1),
            Some(1),
            Some(2),
            None,
            None,
            Some(1),
            Some(1)
          )
        )
      }

      "return a valid SourceFileReport instance if there are some classes which have the same @filename" in {
        val sourceFile = Utils.mkFileFromPath(
          sourceDirA,
          Seq("bar", "foo", "TestSourceFile.scala")
        )
        val fileReport = reader.reportForSource(sourceFile.getCanonicalPath)
        fileReport.file should endWith(
          Seq("bar", "foo", "TestSourceFile.scala").mkString(File.separator)
        )
        fileReport.lineCoverage should equal(
          List(
            None,
            None,
            None,
            Some(1),
            Some(1),
            Some(2),
            None,
            None,
            Some(1),
            Some(1)
          )
        )
      }

      "return a valid SourceFileReport instance if files are in version-specific source dirs" in {
        val sourceFile = Utils.mkFileFromPath(
          sourceDirA212,
          Seq("bar", "foo", "TestSourceScala212.scala")
        )
        println(sourceFile.getCanonicalPath)
        val fileReport = reader.reportForSource(sourceFile.getCanonicalPath)
        fileReport.file should endWith(
          Seq("foo", "TestSourceScala212.scala").mkString(File.separator)
        )
      }
    }
  }

  "CoberturaMultiSourceReader" should {

    "not blow up when DTD documents can't be fetched" in {
      val withoutDTD = new CoberturaMultiSourceReader(
        Utils.mkFileFromPath(resourceDir, Seq("test_cobertura_dtd.xml")),
        sourceDirs,
        Some("UTF-8")
      )
      withoutDTD.reportXML shouldEqual reader.reportXML
    }

    "correctly determine who is parent file and who is child file" in {
      reader.isChild(resourceDir, resourceDir) shouldBe false
      reader.isChild(sourceDirA, sourceDirB) shouldBe false
      reader.isChild(sourceDirA, resourceDir) shouldBe true
    }

    "correctly recognize that paths is not a child only because it is prefix of another path" in {
      // this is an edge case that we are ignoring for now
      // reader.isChild(Utils.mkFileFromPath(resourceDir, Seq("src", "main", "scala-2.12")), Utils.mkFileFromPath(resourceDir, Seq("src", "main", "scala"))) shouldBe false
      // reader.isChild(Utils.mkFileFromPath(resourceDir, Seq("src", "aaab")), Utils.mkFileFromPath(resourceDir, Seq("src", "aaa"))) shouldBe false
      reader.isChild(
        Utils.mkFileFromPath(resourceDir, Seq("src", "aaa", "b")),
        Utils.mkFileFromPath(resourceDir, Seq("src", "aaa"))
      ) shouldBe true
    }
  }

  "CoberturaMultiSourceReader" should {

    "complain when given an empty set of source diectories" in {
      intercept[IllegalArgumentException] {
        new CoberturaMultiSourceReader(resourceDir, Seq(), Some("UTF-8"))
      }
    }

    "complain when at least two of the source directories are nested" in {
      intercept[IllegalArgumentException] {
        new CoberturaMultiSourceReader(
          resourceDir,
          sourceDirs ++ Seq(resourceDir),
          Some("UTF-8")
        )
      }
    }

    "complain when given a non-existing cobertura file" in {
      intercept[FileNotFoundException] {
        new CoberturaMultiSourceReader(resourceDir, sourceDirs, Some("UTF-8"))
      }
    }

    "complain when given am incorrect cobertura file" in {
      intercept[Exception] {
        new CoberturaMultiSourceReader(
          Utils
            .mkFileFromPath(resourceDir, Seq("test_cobertura_corrupted.xml")),
          sourceDirs,
          Some("UTF-8")
        )
      }
    }

  }

  "CoberturaMultiSourceReader" should {
    "correctly split paths to source files" in {
      val sourcePath = Seq("bar", "foo", "TestSourceFile.scala")
      val sourceFile = Utils.mkFileFromPath(sourceDirA, sourcePath)
      val (source, file) = reader.splitPath(sourceFile)
      source shouldEqual sourceDirA.getCanonicalPath
      file shouldEqual sourcePath.mkString(File.separator)
    }

    "complain when given a file that is outside source directories" in {
      intercept[IllegalArgumentException] {
        reader.splitPath(resourceDir)
      }
    }
  }
}
