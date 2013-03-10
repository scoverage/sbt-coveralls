package com.github.theon.coveralls

import sbt.Keys._
import sbt.{Command, Plugin}

/**
 * Date: 10/03/2013
 * Time: 17:01
 */
object CoverallsPlugin extends Plugin {

  override lazy val settings = Seq(commands += coverallCommand)

  val coberturaFile = "target/scala-2.10/coverage-report/cobertura.xml"
  val coverallsFile = "target/scala-2.10/coverage-report/coveralls.json"

  def coverallCommand = Command.args("coveralls", "test") { (state, args) =>
    val arg = args.head

    if (arg == "test") {
      //Run the scct plugin to generate code coverage
      Command.process("scct:test", state)

      val baseDir = state.configuration.baseDirectory.getAbsolutePath

      val reader = new CoberturaReader {
        def file = baseDir + "/" + coberturaFile
      }

      val writer = new CoverallPayloadWriter {
        def file = baseDir + "/" + coverallsFile
      }

      val coverallsClient = new CoverallsClient {}
      val sourceFiles = reader.sourceFilenames()
      writer.start()

      sourceFiles.foreach(sourceFile => {
        val sourceReport = reader.reportForSource(baseDir, sourceFile)
        writer.addSouceFile(sourceReport)
      })

      writer.end()

      val res = coverallsClient.postFile(coverallsFile)
      println(res)
    }

    state
  }
}