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
  
  object TypesafeLogo extends widget.ImagePanel(images.Icons.typesafe)

  title = "Scala Migration Manager"
  preferredSize = (1024, 768)
  minimumSize = preferredSize
  centerFrame
  resizable = false


  protected val mainContainer = new BorderPanel {
    border = EmptyBorder(10)
    
    private val leftPanel = new BorderPanel {
      add(TypesafeLogo, BorderPanel.Position.East)
    }
    add(leftPanel, BorderPanel.Position.North)
    
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
        title = "Exit",
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