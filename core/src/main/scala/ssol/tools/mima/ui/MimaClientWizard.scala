package ssol.tools.mima.ui

import ssol.tools.mima.{ Config, MiMaClient, WriterConfig, MiMaLib }
import wizard._
import scala.tools.nsc.{ util, io }
import util._
import ClassPath._

object MimaClientWizard {
  
  import java.io.File
  
  // data model
  private class PageModel extends WizardPage.Model {
    object Keys {
      val Classpath = "classpath"
      val TableModel = "tableModel"
      val MigrationTargetDir = "targetDir"
      val MigratedJarQualifier = "jarQualifier"
    }

    import Keys._

    def classpath_=(classpath: ClassPath[_]) = data += Classpath -> classpath
    def classpath = data.get(Classpath).get.asInstanceOf[ClassPath[_]]

    def tableModel_=(tableModel: ProblemsModel) = data += TableModel -> tableModel
    def tableModel = data.get(TableModel).get.asInstanceOf[ProblemsModel]
    def hasTableModel = data.get(TableModel).isDefined

    def targetDir_=(target: File) = {
      assert(target.isDirectory)
      data += MigrationTargetDir -> target
    }
    def targetDir = data.get(MigrationTargetDir).get.asInstanceOf[File]
    def hasTargetDir = data.get(MigrationTargetDir).isDefined

    def qualifier_=(qualifier: String) = {
      data += MigratedJarQualifier -> qualifier
    }
    def qualifier = data.get(MigratedJarQualifier).get.asInstanceOf[String]
  }
}

class MimaClientWizard extends Wizard {

  /** Default WizardPage */
  private trait Page extends WizardPage {
    import MimaClientWizard.PageModel
    override val model = new PageModel
  }
  
  // step 1 - select java environment
  this += new JavaEnvironmentPage with Page {
    override def onNext() {
      model.classpath = new JavaClassPath(DefaultJavaContext.classesInPath(cpEditor.classPathString), DefaultJavaContext)
    }
  }
  
  // step 2 - edit project classpath
  this += new ClassPathEditor with Page {
    classpathLabel.text = "Project classpath:"
    
    override def onNext(): Unit = {
      val cp = model.classpath
      model.classpath = new JavaClassPath(DefaultJavaContext.classesInPath(classPathString + io.File.pathSeparator + cp.asClasspathString), DefaultJavaContext)
    }
  }
  
  /*
  // step 3 - report issues
  this += new ReportPage with Page {

    override def onLoad() {
      if (!model.hasTableModel) {
        val mima = new MiMaLib
        val problems = mima.collectProblems(MimaLibWizard.oldLib.get.getAbsolutePath, MimaLibWizard.newLib.get.getAbsolutePath)
        model.tableModel = ProblemsModel(problems)
      }
    }

    override def onReveal() {
      assert(model.hasTableModel)
      setTableModel(model.tableModel)
    }
  }
  
  // step 4 - selected a migration target directory and jar files qualifier
  this += new SaveMigrationPage with Page {
    
    override def onNext() {
      model.targetDir = selectDirectory.selected
      model.qualifier = qualifierPanel.qualifier.text
    }
  }
  */
}