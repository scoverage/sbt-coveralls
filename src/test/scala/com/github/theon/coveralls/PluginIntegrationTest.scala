package com.github.theon.coveralls

import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.ShouldMatchers
import sbt._
import java.io.File

/**
 * Date: 30/03/2013
 * Time: 15:19
 */
class PluginIntegrationTest extends WordSpec with BeforeAndAfterAll with ShouldMatchers {
  object SuccessTestCoverallsPlugin extends AbstractCoverallsPlugin {
    def coberturaFile(state:State) = new File("").getAbsolutePath + "/src/test/resources/test_cobertura.xml"
    def coverallsFile(state:State) = "/tmp/xsbt-coveralls-plugin/coveralls.json"
    def apiHttpClient = new TestSuccessHttpClient()
    def baseDir(state:State) = ""
  }

  object FailureTestCoverallsPlugin extends AbstractCoverallsPlugin {
    def coberturaFile(state:State) = new File("").getAbsolutePath + "/src/test/resources/test_cobertura.xml"
    def coverallsFile(state:State) = "/tmp/xsbt-coveralls-plugin/coveralls.json"
    def apiHttpClient = new TestFailureHttpClient()
    def baseDir(state:State) = ""
  }

  "Coveralls Plugin" when {

    "Run happy path" should {
      "display successful output" in {
        val logger = new TestLogger()
        val state = State(null, Seq(), Set(), None, Seq(), null, null, new GlobalLogging(logger, null, null), null)

        SuccessTestCoverallsPlugin.coverallsCommand.apply(state, Nil)

        logger.messages(Level.Info) should contain("Uploading to coveralls.io succeeded: test message")
        logger.messages(Level.Info) should contain("https://github.com/theon/xsbt-coveralls-plugin")
      }
    }

    "API fails" should {
      "display successful failure message" in {
        val logger = new TestLogger()
        val state = State(null, Seq(), Set(), None, Seq(), null, null, new GlobalLogging(logger, null, null), null)

        FailureTestCoverallsPlugin.coverallsCommand.apply(state, Nil)

        logger.messages(Level.Error) should contain("Uploading to coveralls.io failed: test error message when there was an error")
      }
    }
  }
}

class TestLogger extends Logger {
  var logMessages = Map[Level.Value,Vector[String]]()

  def log(level: Level.Value, message: => String) {
    //Not ThreadSafe!
    val list = logMessages.getOrElse(level, Vector()) :+ message
    logMessages += (level -> list)
  }

  def messages(level: Level.Value) = logMessages(level)

  def trace(t: => Throwable) {}
  def success(message: => String) {}
}
