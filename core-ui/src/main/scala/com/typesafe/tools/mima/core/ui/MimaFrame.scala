package com.typesafe.tools.mima.core.ui

import scala.swing._
import Swing._
import com.typesafe.tools.mima.core.ui._
import com.typesafe.tools.mima.core.ui.wizard._
import com.typesafe.tools.mima.core.Config
import com.typesafe.tools.mima.core.ui.event.ExitMiMa
import com.typesafe.tools.mima.core.buildinfo.BuildInfo

import scala.tools.nsc.{ util, io }
import util._
import ClassPath._

abstract class MimaFrame extends MainFrame with Centered {
  import com.typesafe.tools.mima.core.util.Urls._

  object ScalaLogo extends widget.LinkImagePanel(ScalaSite, images.Icons.scalaLogo)

  object TypesafeLogo extends widget.LinkImagePanel(TypesafeSite, images.Icons.typesafe)

  title = "Migration Manager - " + BuildInfo.version
  preferredSize = (1024, 768)
  minimumSize = preferredSize
  location = center
  resizable = false

  protected val mainContainer = new BorderPanel {
    border = EmptyBorder(10)

    private val topPanel = new BorderPanel {
      add(ScalaLogo, BorderPanel.Position.West)
      add(TypesafeLogo, BorderPanel.Position.East)
    }
    add(topPanel, BorderPanel.Position.North)

    def setContent(c: Component): Unit = {
      c.border = EmptyBorder(10, 0, 0, 0)
      add(c, BorderPanel.Position.Center)
      revalidate()
      repaint()
    }
  }

  override def closeOperation() { showCloseDialog() }

  menuBar = MimaMenuBar

  contents = mainContainer

  peer.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE)

  listenTo(menuBar)

  reactions += {
    case ExitMiMa => showCloseDialog()
  }

  private def showCloseDialog() {
    Dialog.showConfirmation(parent = null,
      icon = images.Icons.exit,
      title = "Exit MiMa",
      message = "Are you sure you want to quit?") match {
        case Dialog.Result.Ok => exit(0)
        case _ => ()
      }
  }

  protected def startWizard(wizard: Wizard) {
    listenTo(wizard)
    wizard.start()
    mainContainer.setContent(wizard)
  }
}