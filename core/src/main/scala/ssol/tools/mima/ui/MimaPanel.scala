package ssol.tools.mima.ui

import scala.swing._
import Swing._
import wizard.Wizard

class MimaPanel extends MainFrame {

  title = "Scala Bytecode Migration Manager"
  preferredSize = (600, 400)
  minimumSize = preferredSize
  //resizable = false
  location = centered

  def centered = {
    import java.awt.Toolkit;
    import java.awt.Dimension;
    val tk = Toolkit.getDefaultToolkit();
    val screenSize = tk.getScreenSize();
    val screenHeight = screenSize.height;
    val screenWidth = screenSize.width;
    ((screenWidth - preferredSize.width) / 2,
      (screenHeight - preferredSize.height) / 2);
  }

  private val mainContainer = new BorderPanel {
    border = EmptyBorder(10)
    add(ScalaInstall, BorderPanel.Position.West)

    def setContent(c: Component): Unit = add(c, BorderPanel.Position.Center)
  }

  contents = mainContainer

  mainContainer.setContent(WelcomeScreen)

  /*
  lazy val reportPage = new ReportPage

  val wizard = new Wizard {
    pages += configurationPage
    pages += reportPage

    switchTo(0)
  }

  mainContainer.setContent(wizard)
  listenTo(wizard)
  */

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