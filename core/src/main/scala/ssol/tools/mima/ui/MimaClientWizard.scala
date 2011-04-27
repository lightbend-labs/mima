package ssol.tools.mima.ui

import scala.swing._
import ssol.tools.mima.{ Config, MiMaClient, WriterConfig, MiMaLib }
import ssol.tools.mima.ui.page._
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
      val MigrationTargetDir = "targetDir"
      val MigratedJarQualifier = "jarQualifier"
    }

    import Keys._

    def classpath_=(classpath: JavaClassPath) = data += Classpath -> classpath
    def classpath = data.get(Classpath).get.asInstanceOf[JavaClassPath]

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

  // step 2 - edit project classpath
  this += new ProjectConfigurationPage with Page {

    override def onNext(): Unit = {
      val cp = model.classpath
      model.classpath = new JavaClassPath(DefaultJavaContext.classesInPath(cpEditor.classPathString + io.File.pathSeparator + cp.asClasspathString), DefaultJavaContext)
    }
  }

  // step 3 - selected a migration target directory and jar files qualifier
  this += new SaveMigrationPage with Page {

    override def onNext() {
      model.targetDir = selectDirectory.selected
      model.qualifier = qualifierPanel.qualifier.text
    }
  }

  // step 5 - carry out the migration
  this += new ThanksPage with Page {
    override def onLoad() {
      val fixes = MiMaClient(model.classpath)
      fixes match {
        case Nil => Swing onEDT { thanksLabel.text = "Your library is already binary compatible. Nothing to fix." }

        case _ =>
          val config = new WriterConfig(model.targetDir, model.qualifier)
          new ssol.tools.mima.Writer(fixes, config).writeOut()
      }
    }
  }
}