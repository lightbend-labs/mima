package ssol.tools.mima.core.util

import java.net.{ URL, URI }

trait BrowserProxy {
  def open(webpage: URI)
  def cannotOpen(webpage: URI, e: Exception)
  def cannotOpen(msg: String, e: Exception)
}

object Browse {
  class BrowserProxyImpl extends BrowserProxy {
    import scala.swing.Dialog
    import ssol.tools.mima.core.ui.images

    override def open(webpage: URI) {
      java.awt.Desktop.getDesktop().browse(webpage)
    }

    override def cannotOpen(webpage: URI, e: Exception) {
      cannotOpen("I can't open the page.\nPlease paste " + webpage.toString + " in your browser", e)
    }

    override def cannotOpen(msg: String, e: Exception) {
      //FIXME: Should log exception!
      Dialog.showMessage(message = msg, title = "Cannot open link", icon = images.Icons.broken)
    }
  }

  private val browser = new Browse(new BrowserProxyImpl)
  
  def to(webpage: String) { browser to webpage }
  def to(webpage: URL) { browser to webpage }
  def to(webpage: URI) { browser to webpage }
}

private[util] class Browse(private val browserProxy: BrowserProxy) {
  import java.io.IOException
  import java.net.URISyntaxException
  import browserProxy._

  def to(webpage: String) { this(webpage) }
  def to(webpage: URL) { this(webpage) }
  def to(webpage: URI) { this(webpage) }

  def apply(webpage: String) {
    try {
      this(new URI(webpage))
    } catch {
      case e: URISyntaxException =>
        cannotOpen("`" + webpage + "` is invalid", e)
    }
  }

  def apply(webpage: URL) {
    try {
      this(webpage.toURI)
    } catch {
      case e: URISyntaxException =>
        cannotOpen("`" + webpage + "` is invalid", e)
    }
  }

  def apply(webpage: URI) {
    try {
      open(webpage)
    } catch {
      case e: UnsupportedOperationException => cannotOpen(webpage, e)
      case e: IOException                   => cannotOpen(webpage, e)
      case e: SecurityException             => cannotOpen(webpage, e)
      case e: IllegalArgumentException      => cannotOpen(webpage, e)
    }
  }
}