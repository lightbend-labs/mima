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

  /** Runs MiMa and returns a two lists of potential binary incompatibilities,
      the first for backward compatibility checking, and the second for forward checking. */
  def runMima(prev: File, curr: File, cp: sbt.Keys.Classpath,
              dir: String, s: TaskStreams): (List[core.Problem],List[core.Problem]) = {
    val mimaLib = makeMima(cp, s)
    (dir match {
       case "backward" | "backwards" | "both" => mimaLib.collectProblems(prev.getAbsolutePath, curr.getAbsolutePath)
       case _ => Nil
     },
     dir match {
       case "forward" | "forwards" | "both" => mimaLib.collectProblems(curr.getAbsolutePath, prev.getAbsolutePath)
       case _ => Nil
     })
  }

  /** Reports binary compatibility errors.
   *  @param failOnProblem if true, fails the build on binary compatibility errors.
   */
  def reportErrors(problemsInFiles: List[(File, List[core.Problem], List[core.Problem])], failOnProblem: Boolean,
        backwardFilters: Seq[core.ProblemFilter], forwardFilters: Seq[core.ProblemFilter], s: TaskStreams, projectName: String): Unit = {
    // filters * found is n-squared, it's fixable in principle by special-casing known
    // filter types or something, not worth it most likely...

    def isReported(problem: core.Problem, filters: Seq[core.ProblemFilter]) = filters forall { f =>
      if (f(problem)) {
        true
      } else {
        s.log.debug(projectName + ": filtered out: " + problem.description + "\n  filtered by: " + f)
        false
      }
    }

    problemsInFiles foreach { case (file, backward, forward) =>
      val backErrors = backward filter (isReported(_, backwardFilters))
      val forwErrors = forward filter (isReported(_, forwardFilters))

      val filteredCount = backward.size + forward.size - backErrors.size - forwErrors.size
      val filteredNote = if (filteredCount > 0) " (filtered " + filteredCount + ")" else ""

      // TODO - Line wrapping an other magikz
      def prettyPrint(p: core.Problem, affected: String): String = {
        " * " + p.description(affected) + p.howToFilter.map("\n   filter with: " + _).getOrElse("")
      }
      s.log.info(s"$projectName: found ${backErrors.size+forwErrors.size} potential binary incompatibilities while checking against $file $filteredNote")
      ((backErrors map {p: core.Problem => prettyPrint(p,"current")}) ++
       (forwErrors map {p: core.Problem => prettyPrint(p,"other")})) foreach { ps =>
        if (failOnProblem) s.log.error(ps)
        else s.log.warn(ps)
      }
      if (failOnProblem && !backErrors.isEmpty && !forwErrors.isEmpty) sys.error(projectName + ": Binary compatibility check failed!")
    }
  }
  /** Resolves an artifact representing the previous abstract binary interface
   *  for testing.
   */
  def getPreviousArtifact(m: ModuleID, ivy: IvySbt, s: TaskStreams): File = {
    val moduleSettings = InlineConfiguration(
      "dummy" % "test" % "version",
      ModuleInfo("dummy-test-project-for-resolving"),
      dependencies = Seq(m))
    val module = new ivy.Module(moduleSettings)
    val report = IvyActions.update(
      module,
      new UpdateConfiguration(
        retrieve = None,
        missingOk = false,
        logging = UpdateLogging.DownloadOnly),
      s.log)
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
