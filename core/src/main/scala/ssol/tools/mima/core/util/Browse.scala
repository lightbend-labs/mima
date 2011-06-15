package ssol.tools.mima.core.util

import java.net.{ URL, URI }

/** Interface for the browser */
trait BrowserProxy {
  /** Ask to open the browser with the passed `webpage` */
  def open(webpage: URI)

  /** Called if the browser canot be launched or the
   *  `webpage` cannot be opened.
   */
  def cannotOpen(webpage: URI, e: Exception)
  def cannotOpen(msg: String, e: Exception)
}

/** Proxy object that can open the user's default browser with the
 *  passed `webpage`.
 */
object Browse {

  /** Boundary class for requesting opening of a `webpage` and
   *  handling failure.
   */
  private class BrowserProxyImpl extends BrowserProxy {
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

  def to(webpage: String) { this(webpage) }
  def to(webpage: URL) { this(webpage) }
  def to(webpage: URI) { this(webpage) }

  def apply(webpage: String) { browser to webpage }
  def apply(webpage: URL) { browser to webpage }
  def apply(webpage: URI) { browser to webpage }
}

/** Allows to launch the user's default browser and open the `webpage` */
private[util] class Browse(private val browserProxy: BrowserProxy) {
  import java.io.IOException
  import java.net.URISyntaxException
  import browserProxy._

  def to(webpage: String) {
    try {
      to(new URI(webpage))
    } catch {
      case e: URISyntaxException =>
        cannotOpen("`" + webpage + "` is invalid", e)
    }
  }

  def to(webpage: URL) {
    try {
      to(webpage.toURI)
    } catch {
      case e: URISyntaxException =>
        cannotOpen("`" + webpage + "` is invalid", e)
    }
  }

  def to(webpage: URI) {
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