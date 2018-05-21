package com.github.theon.coveralls

import java.io.{ FileNotFoundException, File }

import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import org.scoverage.coveralls.{ CoberturaMultiSourceReader }

class CoberturaMultiSourceReaderTest extends WordSpec with BeforeAndAfterAll with Matchers {

  val root = new File(getClass.getResource("/").getFile)
  val srcBarFoo = new File(root, "srcA")
  val srcFoo = new File(root, "srcB")
  val fileBarFoo = new File(srcBarFoo, "bar/foo/TestSourceFile.scala")
  val fileFooPath = "foo/TestSourceFile.scala".replace("/", File.separator)
  val fileFoo = new File(srcFoo, fileFooPath)

  val reader = new CoberturaMultiSourceReader(
    new File(root, "test_cobertura_multisource.xml"),
    Seq(srcBarFoo, srcFoo),
    Some("UTF-8")
  )

  "CoberturaMultiSourceReader" when {
    "reading a Cobertura file" should {

      "list the correct source files" in {
        reader.sourceFiles.toSet shouldEqual Set(fileBarFoo, fileFoo)
      }

      "return a valid SourceFileReport instance" in {
        val fileReport = reader.reportForSource(fileFoo.getCanonicalPath)
        fileReport.file should endWith("foo/TestSourceFile.scala")
        fileReport.lineCoverage should equal(
          List(None, None, Some(1), Some(1), Some(1), None, None, None, None, None)
        )
      }

      "return a valid SourceFileReport instance if there are some classes which have the same @filename" in {
        val fileReport = reader.reportForSource(fileBarFoo.getCanonicalPath)
        fileReport.file should endWith("bar/foo/TestSourceFile.scala")
        fileReport.lineCoverage should equal(
          List(None, None, None, Some(1), Some(1), Some(2), None, None, Some(1), Some(1))
        )
      }
    }
  }

  "CoberturaMultiSourceReader" should {

    "not blow up when DTD documents can't be fetched" in {
      val withoutDTD = new CoberturaMultiSourceReader(
        new File(root, "test_cobertura_dtd.xml"),
        Seq(srcBarFoo, srcFoo),
        Some("UTF-8")
      )
      withoutDTD.reportXML shouldEqual reader.reportXML
    }

    "correctly determine who is parent file and who is child file" in {
      reader.isChild(srcFoo, srcFoo) shouldBe false
      reader.isChild(srcBarFoo, srcFoo) shouldBe false
      reader.isChild(srcFoo, srcBarFoo) shouldBe false
      reader.isChild(srcFoo, root) shouldBe true
      reader.isChild(root, srcFoo) shouldBe false
      reader.isChild(fileFoo, srcFoo) shouldBe true
      reader.isChild(srcFoo, fileFoo) shouldBe false
      //catches mistakes with substrings
      reader.isChild(new File(root, "srcB-2.10"), srcFoo) shouldBe false
    }

    "correctly recognize that paths is not a child only because it is prefix of another path" in {
      reader.isChild(new File(root, "src/main/scala-2.12"), new File(root, "src/main/scala")) shouldBe false
      reader.isChild(new File(root, "src/aaab"), new File(root, "src/aaa")) shouldBe false

      reader.isChild(new File(root, "src/aaa/b"), new File(root, "src/aaa")) shouldBe true
    }
  }

  "CoberturaMultiSourceReader" should {

    "complain when given an empty set of source diectories" in {
      intercept[IllegalArgumentException] {
        new CoberturaMultiSourceReader(new File(""), Seq(), Some("UTF-8"))
      }
    }

    "complain when al least two of the source directories are nested" in {
      intercept[IllegalArgumentException] {
        new CoberturaMultiSourceReader(new File(""), Seq(srcBarFoo, srcFoo, root), Some("UTF-8"))
      }
    }

    "complain when given a non-existing cobertura file" in {
      intercept[FileNotFoundException] {
        new CoberturaMultiSourceReader(new File("zzz"), Seq(root), Some("UTF-8"))
      }
    }

    "complain when given am incorrect cobertura file" in {
      intercept[Exception] {
        new CoberturaMultiSourceReader(fileBarFoo, Seq(root), Some("UTF-8"))
      }
    }

  }

  "CoberturaMultiSourceReader" should {
    "correctly split paths to source files" in {
      val (src, file) = reader.splitPath(fileFoo)
      src shouldEqual srcFoo.getCanonicalPath
      file shouldEqual fileFooPath
    }

    "complain when given a file that is outside source directories" in {
      intercept[IllegalArgumentException] {
        reader.splitPath(root)
      }
    }
  }

}
