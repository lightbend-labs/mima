package ssol.tools.mima.lib.ui.page

import scala.swing._
import scala.swing.Swing._
import javax.swing.event._
import javax.swing._

import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.HTMLDocument

import ssol.tools.mima.core.ui.WithConstraints

class WelcomePage extends GridBagPanel with WithConstraints {
  private class Editor extends JEditorPane(new HTMLEditorKit().getContentType(), "") {
    /**
     * add a CSS rule to force body tags to use the default label font
     * instead of the value in javax.swing.text.html.default.css
     */
    val defaultLabelFont = UIManager.getFont("Label.font");
    val bodyRule = "body { font-family: " + defaultLabelFont.getFamily() + "; " +
      "font-size: " + defaultLabelFont.getSize() + "pt; }";
    (getDocument().asInstanceOf[HTMLDocument]).getStyleSheet().addRule(bodyRule);

    setOpaque(false)
    setEditable(false)

    addHyperlinkListener(new HyperlinkListener() {
      def hyperlinkUpdate(he: HyperlinkEvent) {
        val tpe = he.getEventType()

        if (tpe == HyperlinkEvent.EventType.ACTIVATED)
          java.awt.Desktop.getDesktop().browse(he.getURL().toURI())
      }
    })
  }

  private def createTitledComponent(title: String, component: javax.swing.JComponent) = new Component {
    border = TitledBorder(LineBorder(java.awt.Color.lightGray, 1), title)
    override lazy val peer = component
  }

  private val mimaStepByStepIntroUrl = "http://typesafe.com/technology/migration-manager"
  private val assemblaBugReportUrl = "https://www.assembla.com/spaces/mima/tickets"

  private val defaultLabelFont = javax.swing.UIManager.getFont("Label.font")

  private val columns = 2

  
  withConstraints(gridx = 0, gridy = 0, weightx = 1, gridwidth = columns, anchor = GridBagPanel.Anchor.Center) {
    add(new FlowPanel(FlowPanel.Alignment.Center)(new Label("Welcome to the Scala Migration Manager") {
      font = new java.awt.Font(defaultLabelFont.getName, defaultLabelFont.getStyle, 20)
    }), _)
  }

  withConstraints(gridx = 0, gridy = 1, weightx = 1, gridwidth = columns, fill = GridBagPanel.Fill.Horizontal) {
    add(VStrut(50), _)
  }

  private val intro = new Editor {
    val text = """The Migration Manager (MiMa in short) can help you diagnose binary 
    		 |incompatiblities between two subsequent versions of a same library.<br>
    		 |If this is the first time you use MiMa, we suggest 
             |you read the <a href="%s">step-by-step introduction</a>.""".format(mimaStepByStepIntroUrl).stripMargin

    setText(text)
  }

  withConstraints(gridx = 0, gridy = 2, weightx = 1, fill = GridBagPanel.Fill.Horizontal) {
    add(createTitledComponent("Introduction", intro), _)
  }

  withConstraints(gridx = 0, gridy = 3, weightx = 1, gridwidth = columns, fill = GridBagPanel.Fill.Horizontal) {
    add(VStrut(50), _)
  }
  
  private val bugs = new Editor {
    val text = """Please report issues by filing a new ticket <a href="%s">here</a>. 
    		 |You may use the same service to request feature enhancement.""".format(assemblaBugReportUrl).stripMargin

    setText(text)
  }

  withConstraints(gridx = 0, gridy = 4, weightx = 1, fill = GridBagPanel.Fill.Horizontal) {
    add(createTitledComponent("Report Issues", bugs), _)
  }

  withConstraints(gridx = 0, gridy = 5, weighty = 1, gridwidth = columns, fill = GridBagPanel.Fill.Both) {
    add(Swing.VGlue, _)
  }
}