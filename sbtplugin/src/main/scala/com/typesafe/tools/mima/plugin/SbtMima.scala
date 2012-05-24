package com.typesafe.tools.mima
package plugin

import sbt._
import sbt.Keys.TaskStreams
import scala.tools.nsc.util.JavaClassPath
import com.typesafe.tools.mima.core.util.log.Logging
import scala.tools.nsc.util.DirectoryClassPath
import core.DefaultJavaContext

/** Wrapper on SBT logging for MiMa */
class SbtLogger(s: TaskStreams) extends Logging {
  // Mima is prety chatty
  def info(str: String): Unit = s.log.debug(str)
  def debugLog(str: String): Unit = s.log.debug(str)
}

object SbtMima {
  val x = sbt.Keys.fullClasspath
  
  /** Creates a new MiMaLib object to run analysis. */
  private def makeMima(cp: sbt.Keys.Classpath, s: TaskStreams): lib.MiMaLib = {
    // TODO: Fix MiMa so we don't have to hack this bit in.
    core.Config.setup("sbt-mima-plugin", Array.empty)
    val cpstring = cp map (_.data.getAbsolutePath()) mkString System.getProperty("path.separator")
    val classpath = new JavaClassPath(DefaultJavaContext.classesInPath(cpstring).toIndexedSeq, DefaultJavaContext)
    new lib.MiMaLib(classpath, new SbtLogger(s))
  }
  
  /** Runs MiMa and returns a list of potential binary incompatibilities. */
  def runMima(prev: File, curr: File, cp: sbt.Keys.Classpath, s: TaskStreams): List[core.Problem] = 
    makeMima(cp, s).collectProblems(prev.getAbsolutePath, curr.getAbsolutePath)

  /** Reports binary compatibility errors.
   * @param failOnProblem if true, fails the build on binary compatibility errors.
   */
  def reportErrors(errors: List[core.Problem], failOnProblem: Boolean, s: TaskStreams): Unit = {
    // TODO - Line wrapping an other magikz 
    def prettyPrint(p: core.Problem): String = " * " + p.description
    s.log.info("Found " + errors.size + " potential binary incompatibilities")
    errors map prettyPrint foreach { p =>
      if(failOnProblem) s.log.error(p)
      else              s.log.warn(p)
    }
    if(failOnProblem && !errors.isEmpty) sys.error("Binary compatibility check failed!")
  }
  /** Resolves an artifact representing the previous abstract binary interface
   * for testing.
   */
  def getPreviousArttifact(m: ModuleID, ivy: IvySbt, s: TaskStreams): File = {
    val moduleSettings = InlineConfiguration(
      "dummy" % "test" % "version",
      ModuleInfo("dummy-test-project-for-resolving"),
      dependencies = Seq(m)
    )
    val module = new ivy.Module(moduleSettings)
    val report = IvyActions.update(
      module,
      new UpdateConfiguration(
          retrieve = None, 
          missingOk = false,
          logging = UpdateLogging.DownloadOnly),
      s.log    
    )
    val optFile = (for {
      config <- report.configurations
      module <- config.modules
      (artifact, file) <- module.artifacts
      // TODO - Hardcode this?
      if artifact.name == m.name
    } yield file).headOption
    optFile getOrElse sys.error("Could not resolve previous ABI: " + m)
  }
}
