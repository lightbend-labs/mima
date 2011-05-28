package ssol.tools.mima.core.ui

import scala.swing._
import Swing._
import ssol.tools.mima.core.ui._
import ssol.tools.mima.core.ui.wizard._
import ssol.tools.mima.core.Config
import ssol.tools.mima.core.ui.page._
import event.Event

import scala.tools.nsc.{ util, io }
import util._
import ClassPath._

abstract class MimaFrame extends MainFrame with Centered {

  val ScalaWebsite = "http://www.scala-lang.org/"
  object ScalaLogo extends widget.LinkImagePanel(ScalaWebsite, images.Icons.scalaLogo)
  
  val TypesafeWebsite = "http://typesafe.com/"
  object TypesafeLogo extends widget.LinkImagePanel(TypesafeWebsite, images.Icons.typesafe)

  // FIXME: How can I inject the version number?
  title = "Migration Manager - 0.0.1-beta" 
  preferredSize = (1024, 768)
  minimumSize = preferredSize
  centerFrame
  resizable = false

  protected val mainContainer = new BorderPanel {
    border = EmptyBorder(10)

    private val topPanel = new BorderPanel {
      add(ScalaLogo, BorderPanel.Position.West)
      /*add(new Label(title) {
        font = new Font("Serif", java.awt.Font.ITALIC, 18);
      }, BorderPanel.Position.Center)*/
      add(TypesafeLogo, BorderPanel.Position.East)
      //add(new Separator, BorderPanel.Position.South)
    }
    add(topPanel, BorderPanel.Position.North)

    def setContent(c: Component): Unit = {
      c.border = EmptyBorder(10, 0, 0, 0)
      add(c, BorderPanel.Position.Center)
      revalidate()
      repaint()
    }
  }

  contents = mainContainer

  reactions += {
    case Exit =>
      Dialog.showConfirmation(parent = null,
        icon = images.Icons.exit,
        title = "Exit MiMa",
        message = "Are you sure you want to quit?") match {
          case Dialog.Result.Ok => exit(0)
          case _                => ()
        }
  }

  protected def startWizard(wizard: Wizard) {
    listenTo(wizard)
    wizard.start()
    mainContainer.setContent(wizard)
  }
}