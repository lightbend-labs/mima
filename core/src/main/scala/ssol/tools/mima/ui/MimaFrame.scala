package ssol.tools.mima.ui

import scala.swing._
import Swing._
import wizard.Wizard
import ssol.tools.mima.{Config, MiMaLib}
import event.Event

case object Exit extends Event

class MimaFrame extends MainFrame {

  object ScalaInstall extends ImagePanel(images.Icons.install)

  title = "Scala Bytecode Migration Manager"
  preferredSize = (600, 403)
  minimumSize = preferredSize
  resizable = false
  location = center

  def center = {
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

    def setContent(c: Component): Unit = {
      add(c, BorderPanel.Position.Center)
      revalidate()
      repaint()
    }
  }

  contents = mainContainer

  mainContainer.setContent(WelcomeScreen)

  listenTo(WelcomeScreen)
  
  reactions += {
    case WelcomeScreen.MigrateBinaries => ()

    case WelcomeScreen.CheckIncompatibilities =>
      val reportPage = new ReportPage
      val wizard = new Wizard {
        pages += new ConfigurationPanel(Config.baseClassPath, Config.oldLib, Config.newLib) {
          override def onNext(): Unit = {
            Config.baseClassPath = classPath
            val mima = new MiMaLib
            reportPage.doCompare(oldFile.getAbsolutePath, newFile.getAbsolutePath, mima)
          }
          
          override def onBack(): Unit = {
            mainContainer.setContent(WelcomeScreen)
            MimaFrame.this.deafTo(this)
          }
        }
        
        pages += reportPage
      }

      mainContainer.setContent(wizard)
      listenTo(wizard)
      wizard.start()
  }

  reactions += {
    case Exit =>
      Dialog.showConfirmation(parent = null,
        title = "Exit Mimalib",
        message = "Are you sure you want to quit?") match {
          case Dialog.Result.Ok => sys.exit(0)
          case _ => ()
        }
  }
}