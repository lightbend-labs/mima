package ssol.tools.mima.ui

import scala.swing._
import Swing._
import wizard._
import ssol.tools.mima.{Config, MiMaLib, Fix, Problem, IncompatibleResultTypeProblem}
import event.Event

import scala.tools.nsc.{ util, io }
import util._
import ClassPath._

case object Exit extends Event

class MimaFrame extends MainFrame with CenterFrame {
  
  object ScalaInstall extends ImagePanel(images.Icons.install)
  
  title = "Scala Migration Manager"
  preferredSize = (800, 600)
  minimumSize = preferredSize
  resizable = true

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

  private var problems: List[IncompatibleResultTypeProblem] = Nil
  private var outFile: java.io.File = _
  
  reactions += {
    case WelcomePage.MigrateProject => ()

    case WelcomePage.MigrateLibrary =>
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
        override lazy val content = new ReportPage
        private var model: ProblemsModel = _
        
        override def beforeDisplay() {
          val mima = new MiMaLib
          val problems = mima.collectProblems(Config.oldLib.get.getAbsolutePath, Config.newLib.get.getAbsolutePath)
          model = ProblemsModel(problems)
          Swing.onEDT { content.model = model }
        }
        
        override def canNavigateForward() = () => { true
          val fileName = Config.newLib.get.getName.stripSuffix(".jar") + "-migrated.jar"
          val file = new File(Config.newLib.get.getParent, fileName)
        	
          val fileChooser = new FileChooser()
          fileChooser.selectedFile = file
        	
          fileChooser.showSaveDialog(content) match {
        	  case FileChooser.Result.Approve => 
        	    outFile = fileChooser.selectedFile
        	    true
        	    
        	  case _ => false
        	}
        	
        }
        
        override def onNext() {
          val problems = model.selectedProblems
          val fixableProblems = problems.toList collect { case x: IncompatibleResultTypeProblem => x }
          assert(problems.size == fixableProblems.size, "Only fixable issues should be selectable. Found "+problems.size+" problems, but only "+fixableProblems.size+" can be fixed")
          MimaFrame.this.problems = fixableProblems
        }
      }
      
      wizard += reportPage
      
      wizard += new WizardPage {
        override lazy val content = new ThanksPage
        override val isBackwardNavigationEnabled = false
        override val isForwardNavigationEnabled = false
        
        override def beforeDisplay() {
          val fixes = Fix.libFixesFor(MimaFrame.this.problems)
          new ssol.tools.mima.Writer(fixes, Map(Config.newLib.get -> outFile)).writeOut()
        }
        
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