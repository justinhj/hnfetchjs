package com.justinhj

import java.net.URI

import moment.Moment

import scala.util.{Failure, Success, Try}

// Miscellaneous utils

object Util {

  // Let's display the time like "2 minutes ago" using moment.js
  // ts is epoch time in seconds
  def timestampToPretty(ts: Int) : String = {

    val epochTimeMS = ts * 1000L

    Moment(epochTimeMS).fromNow()

  }

  // We will display just the hostname of the URL
  // this returns close to what we want but not exactly...
  def getHostName(url: String) : String = {
    if(url.isEmpty) ""
    else {
      Try(new URI(url)) match {
        case Success(uri) =>
          "(" + uri.getHost + ")"
        case Failure(_) =>
          "[parse error]"
      }
    }
  }

}
