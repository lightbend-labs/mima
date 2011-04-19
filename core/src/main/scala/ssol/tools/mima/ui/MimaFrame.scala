package ssol.tools.mima.ui

import scala.swing._
import Swing._
import wizard._
import ssol.tools.mima.{ Config, MiMaLib }
import event.Event

import scala.tools.nsc.{ util, io }
import util._
import ClassPath._

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
      val wizard = new Wizard

      wizard += new WizardPage {
        override lazy val content = new JavaClassPathEditor
        override def onNext() {
          Config.baseClassPath = new JavaClassPath(DefaultJavaContext.classesInPath(content.cpEditor.classPathString), DefaultJavaContext)
        }
        override def onBack() {
          mainContainer.setContent(WelcomeScreen)
          deafTo(wizard)
        }
      }

      wizard += new WizardPage {
        override lazy val content = new ConfigurationPanel(Config.oldLib, Config.newLib)
        override def onNext(): Unit = {
          Config.baseClassPath = new JavaClassPath(DefaultJavaContext.classesInPath(content.cpEditor.classPathString + io.File.pathSeparator + Config.baseClassPath.asClasspathString), DefaultJavaContext)
        }
      }

      wizard += new WizardPage {
        override lazy val content = new ReportPage
        override def beforeDisplay() {
          val mima = new MiMaLib
          content.doCompare(Config.oldLib.get.getAbsolutePath, Config.newLib.get.getAbsolutePath, mima)
        }
      }


      listenTo(wizard)
      mainContainer.setContent(wizard)
      wizard.start()
  }

  reactions += {
    case Exit =>
      Dialog.showConfirmation(parent = null,
        title = "Exit Mimalib",
        message = "Are you sure you want to quit?") match {
          case Dialog.Result.Ok => exit(0)
          case _                => ()
        }
  }
}