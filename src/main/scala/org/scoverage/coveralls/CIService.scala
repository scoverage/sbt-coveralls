package org.scoverage.coveralls

import scala.io.Source
import scala.util.parsing.json.{JSON, JSONObject}

trait CIService {
  def name: String
  def jobId: Option[String]
  def pullRequest: Option[String]
  def currentBranch: Option[String]
}

case object TravisCI extends CIService {
  def name = "travis-ci"
  def jobId = sys.env.get("TRAVIS_JOB_ID")
  def pullRequest = sys.env.get("CI_PULL_REQUEST")

  def currentBranch = sys.env.get("CI_BRANCH")
}

case object GitHubActions extends CIService {
  def name = "github"
  def jobId = sys.env.get("GITHUB_RUN_ID")

  // https://github.com/coverallsapp/github-action/blob/master/src/run.ts#L31-L40
  def pullRequest = for {
    eventName <- sys.env.get("GITHUB_EVENT_NAME") if eventName == "pull_request"
    payloadPath <- sys.env.get("GITHUB_EVENT_PATH")
    payload <- JSON.parseRaw(Source.fromFile(payloadPath, "utf-8").mkString)
    prNumber <- payload.asInstanceOf[JSONObject].obj.get("number")
  } yield prNumber.toString

  def currentBranch = sys.env.get("GITHUB_REF")
}
