package ssol.tools.mima.core.util

import java.net.{ URL, URI, URISyntaxException }
import java.io.IOException

import scala.swing.Dialog
import images.Icons

object Browse {

  def to(webpage: String) { this(webpage) }
  def to(webpage: URL) { this(webpage) }
  def to(webpage: URI) { this(webpage) }

  def apply(webpage: String) {
    try {
      this(new URI(webpage))
    } catch {
      case e: URISyntaxException =>
        cannotOpenBrowser("`" + webpage + "` is invalid", e)
    }
  }

  def apply(webpage: URL) {
    try {
      this(webpage.toURI)
    } catch {
      case e: URISyntaxException =>
        cannotOpenBrowser("`" + webpage + "` is invalid", e)
    }
  }

  def apply(webpage: URI) {
    try {
      open(webpage)
    } catch {
      case e: UnsupportedOperationException => cannotOpenBrowser(webpage, e)
      case e: IOException => cannotOpenBrowser(webpage, e)
      case e: SecurityException => cannotOpenBrowser(webpage, e)
      case e: IllegalArgumentException => cannotOpenBrowser(webpage, e)
    }
  }

  private def open(webpage: URI) {
    java.awt.Desktop.getDesktop().browse(webpage)
  }

  private def cannotOpenBrowser(webpage: URI, e: Exception) {
    cannotOpenBrowser("I can't open the page.\nPlease paste " + webpage.toString + " in your browser", e)
  }

  private def cannotOpenBrowser(msg: String, e: Exception) {
    //FIXME: Should log exception!
    Dialog.showMessage(message = msg, title = "Cannot open link", icon = Icons.broken)
  }
}