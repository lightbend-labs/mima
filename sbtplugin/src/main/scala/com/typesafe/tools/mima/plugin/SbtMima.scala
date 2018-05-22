package com.typesafe.tools.mima
package plugin

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.util.log.Logging
import sbt.Keys.TaskStreams
import sbt._
import librarymanagement.{ UpdateLogging => _, _ }
import ivy._
import internal.librarymanagement._

import scala.io.Source
import scala.util._
import scala.util.matching._

/** Wrapper on SBT logging for MiMa */
class SbtLogger(s: TaskStreams) extends Logging {
  // Mima is prety chatty
  def info(str: String): Unit = s.log.debug(str)
  def debugLog(str: String): Unit = s.log.debug(str)
  def warn(str: String): Unit = s.log.warn(str)
  def error(str: String): Unit = s.log.error(str)
}

object SbtMima {
  val x = sbt.Keys.fullClasspath

  /** Creates a new MiMaLib object to run analysis. */
  private def makeMima(cp: sbt.Keys.Classpath, log: Logging): lib.MiMaLib = {
    // TODO: Fix MiMa so we don't have to hack this bit in.
    core.Config.setup("sbt-mima-plugin", Array.empty)
    val cpstring = cp map (_.data.getAbsolutePath()) mkString System.getProperty("path.separator")
    val classpath = com.typesafe.tools.mima.core.reporterClassPath(cpstring)
    new lib.MiMaLib(classpath, log)
  }

  /** Runs MiMa and returns a two lists of potential binary incompatibilities,
      the first for backward compatibility checking, and the second for forward checking. */
  def runMima(prev: File, curr: File, cp: sbt.Keys.Classpath,
              dir: String, log: Logging): (List[core.Problem], List[core.Problem]) = {
    // MiMaLib collects problems to a mutable buffer, therefore we need a new instance every time
    (dir match {
       case "backward" | "backwards" | "both" => makeMima(cp, log).collectProblems(prev.getAbsolutePath, curr.getAbsolutePath)
       case _ => Nil
     },
     dir match {
       case "forward" | "forwards" | "both" => makeMima(cp, log).collectProblems(curr.getAbsolutePath, prev.getAbsolutePath)
       case _ => Nil
     })
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
                         log: Logging, projectName: String): Unit = {
    // filters * found is n-squared, it's fixable in principle by special-casing known
    // filter types or something, not worth it most likely...

    val backErrors = backward filter isReported(module, filters, backwardFilters)(log, projectName)
    val forwErrors = forward filter isReported(module, filters, forwardFilters)(log, projectName)

    val filteredCount = backward.size + forward.size - backErrors.size - forwErrors.size
    val filteredNote = if (filteredCount > 0) " (filtered " + filteredCount + ")" else ""

    // TODO - Line wrapping an other magikz
    def prettyPrint(p: core.Problem, affected: String): String = {
      " * " + p.description(affected) + p.howToFilter.map("\n   filter with: " + _).getOrElse("")
    }

    log.info(s"$projectName: found ${backErrors.size+forwErrors.size} potential binary incompatibilities while checking against $module $filteredNote")
    ((backErrors map {p: core.Problem => prettyPrint(p, "current")}) ++
     (forwErrors map {p: core.Problem => prettyPrint(p, "other")})) foreach { p =>
      if (failOnProblem) log.error(p)
      else log.warn(p)
    }
    if (failOnProblem && (backErrors.nonEmpty || forwErrors.nonEmpty)) sys.error(projectName + ": Binary compatibility check failed!")
  }

  private[mima] def isReported(module: ModuleID, filters: Seq[core.ProblemFilter], versionedFilters: Map[String, Seq[core.ProblemFilter]])(log: Logging, projectName: String)(problem: core.Problem) = {

    // version string "x.y.z" is converted to an Int tuple (x, y, z) for comparison
    val versionOrdering = Ordering[(Int, Int, Int)].on { version: String =>
      val ModuleVersion = """(\d+)\.?(\d+)?\.?(.*)?""".r
      val ModuleVersion(epoch, major, minor) = version
      val toNumeric = (revision: String) => Try(revision.replace("x", Short.MaxValue.toString).filter(_.isDigit).toInt).getOrElse(0)
      (toNumeric(epoch), toNumeric(major), toNumeric(minor))
    }

    (versionedFilters.collect {
      // get all filters that apply to given module version or any version after it
      case f @ (version, versionFilters) if versionOrdering.gteq(version, module.revision) => versionFilters
    }.flatten ++ filters).forall { f =>
      if (f(problem)) {
        true
      } else {
        log.debugLog(projectName + ": filtered out: " + problem.description + "\n  filtered by: " + f)
        false
      }
    }
  }

  /** Resolves an artifact representing the previous abstract binary interface
   *  for testing.
   */
  def getPreviousArtifact(m: ModuleID, ivy: IvySbt, s: TaskStreams): File = {
    val depRes = IvyDependencyResolution(ivy.configuration)
    val module = depRes.wrapDependencyInModule(m)
    val reportEither = depRes.update(
      module,
      UpdateConfiguration() withLogging UpdateLogging.DownloadOnly,
      UnresolvedWarningConfiguration(),
      s.log
    )
    val report = reportEither.fold(x => throw x.resolveException, x => x)
    val optFile = (for {
      config <- report.configurations
      module <- config.modules
      (artifact, file) <- module.artifacts
      if artifact.name == m.name
      if artifact.classifier.isEmpty
    } yield file).headOption
    optFile getOrElse sys.error("Could not resolve previous ABI: " + m)
  }

  def issueFiltersFromFiles(filtersDirectory: File, fileExtension: Regex, s: TaskStreams): Map[String, Seq[ProblemFilter]] = {
    if (filtersDirectory.exists) loadMimaIgnoredProblems(filtersDirectory, fileExtension, s.log)
    else Map.empty
  }

  def loadMimaIgnoredProblems(directory: File, fileExtension: Regex, logger: Logger): Map[String, Seq[ProblemFilter]] = {
    val ExclusionPattern = """ProblemFilters\.exclude\[([^\]]+)\]\("([^"]+)"\)""".r

    def findFiles(): Seq[(File, String)] = directory.listFiles().flatMap(f => fileExtension.findFirstIn(f.getName).map((f, _)))
    def parseFile(file: File, extension: String): Either[Seq[Throwable], (String, Seq[ProblemFilter])] = {
      val version = file.getName.dropRight(extension.size)

      def parseLine(text: String, line: Int): Try[ProblemFilter] =
        Try {
          text match {
            case ExclusionPattern(className, target) => ProblemFilters.exclude(className, target)
            case x => throw new RuntimeException(s"Couldn't parse '$x'")
          }
        }.transform(Success(_), ex => Failure(new ParsingException(file, line, ex)))

      val (excludes, failures) =
        Source.fromFile(file)
          .getLines()
          .zipWithIndex
          .filterNot { case (str, line) => str.trim.isEmpty || str.trim.startsWith("#") }
          .map((parseLine _).tupled)
          .partition(_.isSuccess)

      if (failures.isEmpty) Right(version -> excludes.map(_.get).toSeq)
      else Left(failures.map(_.failed.get).toSeq)
    }

    require(directory.exists(), s"Mima filter directory did not exist: ${directory.getAbsolutePath}")

    val (mappings, failures) =
      findFiles()
        .map((parseFile _).tupled)
        .partition(_.isRight)

    if (failures.isEmpty)
      mappings
        .map(_.right.get)
        .groupBy(_._1)
        .map { case (version, filters) => version -> filters.flatMap(_._2) }
    else {
      failures.flatMap(_.left.get).foreach(ex => logger.error(ex.getMessage))

      throw new RuntimeException(s"Loading Mima filters failed with ${failures.size} failures.")
    }
  }

  case class ParsingException(file: File, line: Int, ex: Throwable) extends RuntimeException(s"Error while parsing $file, line $line: ${ex.getMessage}", ex)
}
