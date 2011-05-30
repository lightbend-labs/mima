package ssol.tools.mima.core.ui.widget

import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.HTMLDocument

import javax.swing._
import javax.swing.event._

import ssol.tools.mima.core.util.Browse

class HtmlEditorPane extends JEditorPane(new HTMLEditorKit().getContentType(), "") {
  private val defaultLabelFont = UIManager.getFont("Label.font")
  /**
   * add a CSS rule to force body tags to use the default label font
   * instead of the value in javax.swing.text.html.default.css
   */
  val bodyRule = "body { font-family: " + defaultLabelFont.getFamily() + "; " +
    "font-size: " + defaultLabelFont.getSize() + "pt; }";
  (getDocument().asInstanceOf[HTMLDocument]).getStyleSheet().addRule(bodyRule);

  setOpaque(false)
  setEditable(false)

  addHyperlinkListener(new HyperlinkListener() {
    def hyperlinkUpdate(he: HyperlinkEvent) {
      val tpe = he.getEventType()

      if (tpe == HyperlinkEvent.EventType.ACTIVATED)
        Browse to he.getURL()
    }
  })
}