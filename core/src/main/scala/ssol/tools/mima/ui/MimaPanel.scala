package ssol.tools.mima.ui

import scala.swing._
import Swing._
import wizard.Wizard

class MimaPanel extends MainFrame {

  title = "Scala Bytecode Migration Manager"
  location = (300, 250)
  preferredSize = (1000, 500)

  private val container = new BorderPanel {
    border = EmptyBorder(10)
    add(ScalaInstall, BorderPanel.Position.West)

    def replaceCenter(c: Component) = add(c, BorderPanel.Position.Center)
  }

  contents = container
  
  
  lazy val reportPage = new ReportPage

  val wizard = new Wizard {
    //pages += configurationPage
    pages += reportPage

    switchTo(0)
  }

  container.replaceCenter(wizard)
  listenTo(wizard)
  
  
 /*
  reactions += {
    case Next(`reportPage`) =>
      println("Reporting now")
      // set the new settings
      Config.baseClassPath = configurationPage.classPath
      val mima = new MiMaLib
      reportPage.doCompare(configurationPage.oldFile.getAbsolutePath, configurationPage.newFile.getAbsolutePath, mima)

    case Cancelled() =>
      Dialog.showConfirmation(parent = wizard,
        title = "Exit Mimalib",
        message = "Are you sure you want to quit?") match {
          case Dialog.Result.Ok => sys.exit(0)
          case _                => ()
        }
  }
 */
}