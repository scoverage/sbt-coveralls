package org.scoverage.coveralls

import scala.io.Source
import io.circe._
import io.circe.parser
import io.circe.generic.auto._

/** The strategy to use when authenticating against Coveralls.
  */
sealed trait CoverallsAuth

/** Auth strategy where a Coveralls-specific token is used. Works
  * with every CI service.
  */
case class CoverallsRepoToken(token: String) extends CoverallsAuth

/** Auth strategy where a token specific to the CI service is used,
  * such as a GitHub token. Works on selected CI services supported
  * by Coveralls.
  */
case class CIServiceToken(token: String) extends CoverallsAuth

/** Auth strategy where no token is passed. This seems to work
  * for Travis.
  */
case object NoTokenNeeded extends CoverallsAuth
