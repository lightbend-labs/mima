package com.typesafe.tools.mima
package plugin

import com.typesafe.tools.mima.core.Config
import com.typesafe.tools.mima.core.util.log.Logging
import sbt.Keys.TaskStreams
import sbt._

import scala.util.Try

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
    val classpath = com.typesafe.tools.mima.core.reporterClassPath(cpstring)
    new lib.MiMaLib(classpath, new SbtLogger(s))
  }

  /** Runs MiMa and returns a two lists of potential binary incompatibilities,
      the first for backward compatibility checking, and the second for forward checking. */
  def runMima(prev: File, curr: File, cp: sbt.Keys.Classpath,
              dir: String, s: TaskStreams): (List[core.Problem], List[core.Problem]) = {
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
  def reportErrors(problemsInFiles: Map[ModuleID, (List[core.Problem], List[core.Problem])],
                   failOnProblem: Boolean,
                   filters: Seq[core.ProblemFilter],
                   backwardFilters: Map[String, Seq[core.ProblemFilter]],
                   forwardFilters: Map[String, Seq[core.ProblemFilter]],
                   s: TaskStreams, projectName: String): Unit =
    problemsInFiles foreach { case (module, (backward, forward)) =>
      reportModuleErrors(module, backward, forward, failOnProblem, filters, backwardFilters, forwardFilters, s, projectName)
    }

  /** Reports binary compatibility errors for a module.
   *  @param failOnProblem if true, fails the build on binary compatibility errors.
   */
  def reportModuleErrors(module: ModuleID,
                   backward: List[core.Problem],
                   forward: List[core.Problem],
                   failOnProblem: Boolean,
                   filters: Seq[core.ProblemFilter],
                   backwardFilters: Map[String, Seq[core.ProblemFilter]],
                   forwardFilters: Map[String, Seq[core.ProblemFilter]],
                   s: TaskStreams, projectName: String): Unit = {
    // filters * found is n-squared, it's fixable in principle by special-casing known
    // filter types or something, not worth it most likely...

    // version string "x.y.z" is converted to an Int tuple (x, y, z) for comparison
    val versionOrdering = Ordering[(Int, Int, Int)].on { version: String =>
      val ModuleVersion = """(\d+)\.(\d+)\.(.*)""".r
      val ModuleVersion(epoch, major, minor) = version
      val toNumeric = (revision: String) => Try(revision.filter(_.isDigit).toInt).getOrElse(0)
      (toNumeric(epoch), toNumeric(major), toNumeric(minor))
    }

    def isReported(module: ModuleID, verionedFilters: Map[String, Seq[core.ProblemFilter]])(problem: core.Problem) = (verionedFilters.collect {
      // get all filters that apply to given module version or any version after it
      case f @ (version, filters) if versionOrdering.gteq(version, module.revision) => filters
    }.flatten ++ filters).forall { f =>
      if (f(problem)) {
        true
      } else {
        s.log.debug(projectName + ": filtered out: " + problem.description + "\n  filtered by: " + f)
        false
      }
    }

    val backErrors = backward filter isReported(module, backwardFilters)
    val forwErrors = forward filter isReported(module, forwardFilters)

    val filteredCount = backward.size + forward.size - backErrors.size - forwErrors.size
    val filteredNote = if (filteredCount > 0) " (filtered " + filteredCount + ")" else ""

    // TODO - Line wrapping an other magikz
    def prettyPrint(p: core.Problem, affected: String): String = {
      " * " + p.description(affected) + p.howToFilter.map("\n   filter with: " + _).getOrElse("")
    }

    s.log.info(s"$projectName: found ${backErrors.size+forwErrors.size} potential binary incompatibilities while checking against $module $filteredNote")
    ((backErrors map {p: core.Problem => prettyPrint(p, "current")}) ++
     (forwErrors map {p: core.Problem => prettyPrint(p, "other")})) foreach { p =>
      if (failOnProblem) s.log.error(p)
      else s.log.warn(p)
    }
    if (failOnProblem && (backErrors.nonEmpty || forwErrors.nonEmpty)) sys.error(projectName + ": Binary compatibility check failed!")
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
      if artifact.name == m.name
      if artifact.classifier.isEmpty
    } yield file).headOption
    optFile getOrElse sys.error("Could not resolve previous ABI: " + m)
  }
}
