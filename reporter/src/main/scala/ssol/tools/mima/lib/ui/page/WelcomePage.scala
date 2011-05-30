package ssol.tools.mima.lib.ui.page

import scala.swing._
import scala.swing.Swing._
import javax.swing.event._
import javax.swing._

import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.HTMLDocument
import javax.swing.border.TitledBorder

import ssol.tools.mima.core.ui.WithConstraints

class WelcomePage extends GridBagPanel with WithConstraints {
  private class Editor extends JEditorPane(new HTMLEditorKit().getContentType(), "") {
    /**
     * add a CSS rule to force body tags to use the default label font
     * instead of the value in javax.swing.text.html.default.css
     */
    val defaultLabelFont = UIManager.getFont("Label.font");
    val bodyRule = "body { font-family: " + defaultLabelFont.getFamily() + "; " +
      "font-size: " + textFont.getSize() + "pt; }";
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
  

  private val mimaIntroUrl = "http://typesafe.com/technology/migration-manager"
  private val mimaStepByStepGuideUrl = "http://typesafe.com/technology/migration-manager"
  private val assemblaBugReportUrl = "https://www.assembla.com/spaces/mima/tickets"
  
  private val defaultLabelFont = javax.swing.UIManager.getFont("Label.font")
  private val textFont = new java.awt.Font(defaultLabelFont.getName, defaultLabelFont.getStyle, 15)

  private def createTitledComponent(title: String, component: javax.swing.JComponent) = new Component {
    private val darkRed =  new java.awt.Color(130,0,0)
    border = BorderFactory.createTitledBorder(LineBorder(java.awt.Color.lightGray, 1), title, TitledBorder.LEADING, TitledBorder.TOP, textFont, darkRed)
    override lazy val peer = component
  }


  private val columns = 2

  
  private var row = 0 

  withConstraints(gridx = 0, gridy = row, weightx = 1, gridwidth = columns, fill = GridBagPanel.Fill.Horizontal) {
    add(VStrut(30), _)
  }
  
  row += 1
  
  withConstraints(gridx = 0, gridy = row, weightx = 1, gridwidth = columns, anchor = GridBagPanel.Anchor.Center) {
    add(new FlowPanel(FlowPanel.Alignment.Center)(new Label("Welcome to the Scala Migration Manager") {
      font = new java.awt.Font(defaultLabelFont.getFamily, defaultLabelFont.getStyle, 30)
    }), _)
  }
  
  row += 1

  withConstraints(gridx = 0, gridy = row, weightx = 1, gridwidth = columns, fill = GridBagPanel.Fill.Horizontal) {
    add(VStrut(20), _)
  }
  
  row += 1
  
  withConstraints(gridx = 0, gridy = row, weightx = 1, gridwidth = columns, fill = GridBagPanel.Fill.Horizontal) {
    add(new Label("Click `Next` if you want to start now") {font = textFont}, _) 
  }
  
  row += 1
  
  withConstraints(gridx = 0, gridy = row, weightx = 1, gridwidth = columns, fill = GridBagPanel.Fill.Horizontal) {
    add(VStrut(50), _)
  }
  
  row += 1

  private val intro = new Editor {
    val text = """The Migration Manager (MiMa in short) can help you diagnose binary incompatibilities 
                 |between two versions of a same library. This can greatly help you, the library producer, 
    			       |to take the right steps when evolving the API. This enables library's users to confidently 
    					   |upgrade to newer versions without having to recompile, hence taking advantage of all the new 
    					   |features of the latest version with no overhead. <br><br>
    						 |In fact, upgrading a mission critical applications to use a new version of a library can sometimes be 
    					   |a difficult and frustrating task that can put your business at risk. Until now, you were forced 
    						 |to upgrade your own codebase and then consider how to make it binary-compatible with third-party 
    						 |libraries other subsystems on which you rely. Now, if the library's version you want to move to 
    					   |is binary compatible with the one currently in use (and you can check that with MiMa), then 
    						 |upgrading becomes an effortless task that only requires you to replace the old library with 
    						 |the new one. It really can't be easier. <a href="%s">Read more...</a> <br><br>
    		         |If this is the first time you are using MiMa, we suggest you read the <a href="%s">step-by-step guide</a> 
    						 |to get quickly up to speed."""
                 .format(mimaIntroUrl,mimaStepByStepGuideUrl).stripMargin

    setText(text)
  }

  withConstraints(gridx = 0, gridy = row, weightx = 1, fill = GridBagPanel.Fill.Horizontal) {
    add(createTitledComponent("Introduction", intro), _)
  }

  row += 1
  
  withConstraints(gridx = 0, gridy = row, weightx = 1, gridwidth = columns, fill = GridBagPanel.Fill.Horizontal) {
    add(VStrut(50), _)
  }
  
  row += 1
  
  private val bugs = new Editor {
    val text = """A <a href="%s">ticketing system</a> exists to report issues or request new features.""".format(assemblaBugReportUrl).stripMargin

    setText(text)
  }

  withConstraints(gridx = 0, gridy = row, weightx = 1, fill = GridBagPanel.Fill.Horizontal) {
    add(createTitledComponent("Tickets", bugs), _)
  }
  
  row += 1

  withConstraints(gridx = 0, gridy = row, weighty = 1, gridwidth = columns, fill = GridBagPanel.Fill.Both) {
    add(Swing.VGlue, _)
  }
}