package ssol.tools.mima.ui

import scala.swing._

import ssol.tools.mima.{ Config, MiMaLib, WriterConfig }
import wizard._
import scala.tools.nsc.{ util, io }
import util._
import ClassPath._
import ssol.tools.mima.ui.page._

object MimaLibWizard {
  import java.io.File

  // data model
  private class PageModel extends WizardPage.Model {
    object Keys {
      val Classpath = "classpath"
      val OldLib = "oldLib"
      val NewLib = "newLib"
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

  //FIXME: Please remove this doomed code...
  private var oldLib: Option[File] = MimaApp.resargs match { case Nil => None case x :: xs => Some(new File(x)) }
  private var newLib: Option[File] = MimaApp.resargs match { case Nil => None case x :: Nil => None case x :: y :: xs => Some(new File(y)) }
}

/** Wizard for MimaLib */
class MimaLibWizard extends Wizard {

  /** Default WizardPage */
  private trait Page extends WizardPage {
    import MimaLibWizard.PageModel
    override val model = new PageModel
  }

  // step 1 - select java environment
  this += new JavaEnvironmentPage with Page {
    import scala.tools.nsc.{ util, io }
    import util._
    import ClassPath._
    model.classpath = Config.baseClassPath

    override def onReveal() {
      cpEditor.classpath = split(model.classpath.asClasspathString)
    }

    override def onNext() {
      model.classpath = new JavaClassPath(DefaultJavaContext.classesInPath(cpEditor.classPathString), DefaultJavaContext)
    }
  }

  // step 2 - select library
  this += new ConfigurationPanel(MimaLibWizard.oldLib, MimaLibWizard.newLib) with Page {
    override def canNavigateForward = areFilesSelected

    override def onNext(): Unit = {
      val cp = model.classpath
      model.classpath = new JavaClassPath(DefaultJavaContext.classesInPath(cpEditor.classPathString + io.File.pathSeparator + cp.asClasspathString), DefaultJavaContext)
    }

    reactions += {
      // forward navigation allowed only if files have been selected
      case FilesSelected(oldLib, newLib) =>
        MimaLibWizard.oldLib = Some(oldLib)
        MimaLibWizard.newLib = Some(newLib)
        publish(WizardPage.CanGoNext(true))
    }
  }

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
}