import com.github.theon.coveralls.{TestFailureHttpClient, TestSuccessHttpClient}
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.Matchers
import org.scoverage.coveralls.AbstractCoverallsPlugin
import sbt._
import java.io.File

class PluginIntegrationTest extends WordSpec with BeforeAndAfterAll with Matchers {

  val projectRoot = new File("")
  val coberturaFile = new File(projectRoot + "src/test/resources/test_cobertura.xml")
  val coverallsFile = new File("/tmp/xsbt-coveralls-plugin/coveralls.json")

  object SuccessTestCoverallsPlugin extends AbstractCoverallsPlugin {
    def apiHttpClient = new TestSuccessHttpClient()
    def userRepoToken(token: Option[String], tokenFile: Option[String]) = Some("test-repo-token")
    def travisJobIdent = None
  }

  object FailureTestCoverallsPlugin extends AbstractCoverallsPlugin {
    def apiHttpClient = new TestFailureHttpClient()
    def userRepoToken(token: Option[String], tokenFile: Option[String]) = Some("test-repo-token")
    def travisJobIdent = None
  }

  object NoRepoTokenOfTravisJobIdTestCoverallsPlugin extends AbstractCoverallsPlugin {
    def apiHttpClient = new TestFailureHttpClient()
    def userRepoToken(token: Option[String], tokenFile: Option[String]) = None
    def travisJobIdent = None
  }

  "Coveralls Plugin" when {
    //    "Run happy path" should {
    //      "display successful output" in {
    //        val logger = new TestLogger()
    //        val state = State(null, Seq(), Set(), None, Seq(), null, null, new GlobalLogging(logger, null, null,null, null), null)
    //
    //        SuccessTestCoverallsPlugin.coverallsCommand(state, projectRoot, coberturaFile, Nil, coverallsFile, "UTF-8",
    //          None, None, None, "scoverage:test")
    //
    //        logger.messages(Level.Info) should contain("Uploading to coveralls.io succeeded: test message")
    //        logger.messages(Level.Info) should contain("https://github.com/theon/xsbt-coveralls-plugin")
    //      }
    //    }
    //
    //    "API fails" should {
    //      "display successful failure message" in {
    //        val logger = new TestLogger()
    //        val state = State(null, Seq(), Set(), None, Seq(), null, null, new GlobalLogging(logger, null, null,null, null), null)
    //
    //        FailureTestCoverallsPlugin
    //          .coverallsCommand(state,
    //            projectRoot,
    //            coberturaFile,
    //            Nil,
    //            coverallsFile,
    //            "UTF-8",
    //            None,
    //            None,
    //            None,
    //            "scoverage:test")
    //
    //        logger.messages(Level.Error) should contain("Uploading to coveralls.io failed: test error message when there was an error")
    //      }
    //    }
    //
    //    "No Repo Token or Travis Job Id" should {
    //      "display some useful information to the user" in {
    //        val logger = new TestLogger()
    //        val state = State(null, Seq(), Set(), None, Seq(), null, null, new GlobalLogging(logger, null, null, null, null), null)
    //
    //        NoRepoTokenOfTravisJobIdTestCoverallsPlugin
    //          .coverallsCommand(state,
    //            projectRoot,
    //            coberturaFile,
    //            Nil,
    //            coverallsFile,
    //            "UTF-8",
    //            None,
    //            None,
    //            None,
    //            "scoverage:test")
    //
    //        logger.messages(Level.Error) should contain("Could not find coveralls repo token or determine travis job id")
    //      }
    //    }
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
