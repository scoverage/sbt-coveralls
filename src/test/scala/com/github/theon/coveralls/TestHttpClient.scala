package com.github.theon.coveralls

/**
 * Date: 30/03/2013
 * Time: 15:36
 */
class TestSuccessHttpClient extends HttpClient {
  var dataIn:String = _

  def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]) = {
    dataIn = new String(data)
    """
      {
        "message":"test message",
        "error": false,
        "url": "https://github.com/theon/xsbt-coveralls-plugin"
      }
    """
  }
}

class TestFailureHttpClient extends HttpClient {
  def multipart(url: String, name: String, filename: String, mime: String, data: Array[Byte]) = {
    """
      {
        "message":"test error message when there was an error",
        "error": true
      }
    """
  }
}