package org.scoverage.coveralls

class HttpClientTestSuccess extends HttpClient {
  var dataIn: String = _

  def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]) = {
    dataIn = new String(data, "UTF-8")
    new CoverallHttpResponse(
      200,
      """
      {
        "message":"test message",
        "error": false,
        "url": "https://github.com/theon/xsbt-coveralls-plugin"
      }
      """
    )
  }
}

class HttpClientTestFailure extends HttpClient {
  def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]) = {
    new CoverallHttpResponse(
      200,
      """
      {
        "message":"test error message when there was an error",
        "error": true
      }
      """
    )
  }
}

case class HttpClientTestFake(status: Int, body: String) extends HttpClient {
  def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]) = {
    new CoverallHttpResponse(status, body)
  }
}
