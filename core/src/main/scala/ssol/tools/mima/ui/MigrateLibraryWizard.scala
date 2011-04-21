package ssol.tools.mima.ui

import wizard._
import scala.tools.nsc.{ util, io }
import util._
import ClassPath._


class MigrateLibraryWizard extends Wizard {

  this += new JavaClassPathEditor with WizardPage {
    override def onNext() {
      data += "classpath" -> new JavaClassPath(DefaultJavaContext.classesInPath(cpEditor.classPathString), DefaultJavaContext)
    }
  }
  
  
  /*
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
      */
}