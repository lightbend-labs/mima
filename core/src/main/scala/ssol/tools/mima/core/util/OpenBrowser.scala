package ssol.tools.mima.core.util

import java.net.{ URI, URISyntaxException }
import java.io.IOException

object Browse {

  class PageCannotBeOpened(msg: String, e: Exception) extends Exception(msg, e)

  def to(webpage: String): Unit = this(webpage)
  def to(webpage: URI): Unit = this(webpage)
  
  def apply(webpage: String): Unit = {
    try {
      this(new URI(webpage))
    } catch {
      case e: URISyntaxException =>
        throw new PageCannotBeOpened("`%s` is invalid".formatted(webpage), e)
    }
  }

  def apply(webpage: URI): Unit = {
    try {
      open(webpage)
    } catch {
      case e: UnsupportedOperationException => cannotOpenBrowser(webpage, e)
      case e: IOException                   => cannotOpenBrowser(webpage, e)
      case e: SecurityException             => cannotOpenBrowser(webpage, e)
      case e: IllegalArgumentException      => cannotOpenBrowser(webpage, e)
    }
  }
  
  private def open(webpage: URI) = {
    java.awt.Desktop.getDesktop().browse(webpage)
  }

  private def cannotOpenBrowser(webpage: URI, e: Exception): Nothing = {
    throw new PageCannotBeOpened("It seems I can't open the page. Please copy/paste `%s` in your browser".formatted(webpage.toString), e)
  }
}