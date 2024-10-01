package org.scoverage.coveralls

/** The strategy to use when authenticating against Coveralls.
  */
sealed trait CoverallsAuth extends Product with Serializable

/** Auth strategy where a Coveralls-specific token is used. Works with every CI
  * service.
  */
final case class CoverallsRepoToken(token: String) extends CoverallsAuth

/** Auth strategy where a token specific to the CI service is used, such as a
  * GitHub token. Works on selected CI services supported by Coveralls.
  */
final case class CIServiceToken(token: String) extends CoverallsAuth

/** Auth strategy where no token is passed. This seems to work for Travis.
  */
case object NoTokenNeeded extends CoverallsAuth
