package ssol.tools.mima.lib.ui

import scala.swing._

import ssol.tools.mima.core.Config
import ssol.tools.mima.lib.MiMaLib
import ssol.tools.mima.core.ui.wizard._
import ssol.tools.mima.core.ui.model.ReportTableModel
import scala.tools.nsc.{ util, io }
import util._
import ClassPath._
import ssol.tools.mima.core.ui.page._
import ssol.tools.mima.lib.ui.page.WelcomePage

import widget.LicenseAgreementView

object LibWizard {
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

    def classpath_=(classpath: JavaClassPath) = data += Classpath -> classpath
    def classpath: JavaClassPath = data.get(Classpath) match {
      case Some(cp) => cp.asInstanceOf[JavaClassPath]
      case None => Config.baseClassPath
    }

    def tableModel_=(tableModel: ReportTableModel) = data += TableModel -> tableModel
    def tableModel = data.get(TableModel).get.asInstanceOf[ReportTableModel]
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
  private var oldLib: Option[File] = None //MimaApp.resargs match { case Nil => None case x :: xs => Some(new File(x)) }
  private var newLib: Option[File] = None //MimaApp.resargs match { case Nil => None case x :: Nil => None case x :: y :: xs => Some(new File(y)) }
}

/** Wizard for MimaLib */
class LibWizard extends Wizard {

  /** Default WizardPage */
  private trait Page extends WizardPage {
    import LibWizard.PageModel
    override val model = new PageModel
  }
  
  // step 0 - welcome screen
  this += new WelcomePage with Page
  
  // step 1 - license agreement
  this += new LicenseAgreementView with Page {
    private var _licenseAccepted = false
    override def canNavigateForward = _licenseAccepted 
    
    override def canNavigateBack = false
    
    listenTo(this)
    reactions += {
      case LicenseAgreementView.LicenseAccepted(status) =>
        _licenseAccepted = status
        publish(WizardPage.CanGoNext(status))
    }
  }
  
  // step 2 - select library
  this += new ConfigurationPanel(LibWizard.oldLib, LibWizard.newLib) with Page {
    override def canNavigateBack = false
    override def canNavigateForward = areFilesSelected

     override def onReveal() {
      cpEditor.classpath = split(model.classpath.asClasspathString)
    }
    
    override def onNext(): Unit = {
      model.classpath = new JavaClassPath(DefaultJavaContext.classesInPath(cpEditor.classPathString), DefaultJavaContext)
    }

    reactions += {
      // forward navigation allowed only if files have been selected
      case FilesSelected(oldLib, newLib) =>
        LibWizard.oldLib = Some(oldLib)
        LibWizard.newLib = Some(newLib)
        publish(WizardPage.CanGoNext(true))
    }
  }

  // step 3 - report issues
  this += new ReportPage with Page {

    override def onLoad() {
      if (!model.hasTableModel) {
        val mima = new MiMaLib(model.classpath, ssol.tools.mima.core.util.log.UiLogger)
        val problems = mima.collectProblems(LibWizard.oldLib.get.getAbsolutePath, LibWizard.newLib.get.getAbsolutePath)
        model.tableModel = ReportTableModel(problems)
      }
    }

    override def onReveal() {
      assert(model.hasTableModel)
      setTableModel(model.tableModel)
    }
  }
}