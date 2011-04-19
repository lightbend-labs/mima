package ssol.tools.mima.ui

import scala.swing._
import Swing._
import wizard._
import ssol.tools.mima.{Config, MiMaLib}
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

  mainContainer.setContent(WelcomePage)

  listenTo(WelcomePage)

  reactions += {
    case WelcomePage.MigrateBinaries => ()

    case WelcomePage.CheckIncompatibilities =>
      val wizard = new Wizard
      listenTo(wizard)
      
      wizard += new WizardPage {
        override lazy val content = new JavaClassPathEditor
        
        override def onNext() {
          Config.baseClassPath = new JavaClassPath(DefaultJavaContext.classesInPath(content.cpEditor.classPathString), DefaultJavaContext)
        }
        
        override def onBack() {
          deafTo(wizard)
          mainContainer.setContent(WelcomePage)
        }
      }

      wizard += new WizardPage {
        override lazy val content = new ConfigurationPanel(Config.oldLib, Config.newLib)

        isForwardNavigationEnabled = content.areFilesSelected
        
        override def onEntering() = {
          listenTo(content)
          reactions += {
            case FilesSelected(oldLib, newLib) =>
              Config.oldLib = Some(oldLib)
              Config.newLib = Some(newLib)
              isForwardNavigationEnabled = true
          }
        }
        
        override def onLeaving() = {
          deafTo(content)
        }
        
        override def onNext(): Unit = {
          Config.baseClassPath = new JavaClassPath(DefaultJavaContext.classesInPath(content.cpEditor.classPathString + io.File.pathSeparator + Config.baseClassPath.asClasspathString), DefaultJavaContext)
        }
      }

      val reportPage = new WizardPage {
        import java.io.File
        var mima: MiMaLib = null
        override lazy val content = new ReportPage
        
        var outputFile: Option[File] = None
        
        override def beforeDisplay() {
          mima = new MiMaLib
          content.doCompare(Config.oldLib.get.getAbsolutePath, Config.newLib.get.getAbsolutePath, mima)
        }
        
        override def canNavigateForward() = () => {
          val fileName = Config.oldLib.get.getName.stripSuffix(".jar") + "-migrated.jar"
          val file = new File(Config.oldLib.get.getParent, fileName)
        	
          val fileChooser = new FileChooser()
          fileChooser.selectedFile = file
        	
          fileChooser.showSaveDialog(content) match {
        	  case FileChooser.Result.Approve => 
        	    outputFile = Some(fileChooser.selectedFile)
        	    true
        	    
        	  case _ => false
        	}
        }
        
        override def onNext() {
          //mima.fixesFor
        }
      }
      
      wizard += reportPage
      
      wizard += new WizardPage {
        override val isBackwardNavigationEnabled = false
        override lazy val content = new ThanksPage
      }
      
      wizard.start()
      
      mainContainer.setContent(wizard)
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