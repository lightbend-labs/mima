package com.typesafe.tools.mima
package plugin

import java.io.File

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.util.log.Logging
import com.typesafe.tools.mima.lib.MiMaLib
import sbt._
import sbt.Keys.{ Classpath, TaskStreams }
import sbt.librarymanagement.{ UpdateLogging => _, _ }
import sbt.librarymanagement.ivy._
import sbt.internal.librarymanagement._

import scala.io.Source
import scala.tools.nsc.util.ClassPath
import scala.util._
import scala.util.control.NonFatal
import scala.util.matching._

object SbtMima {
  /** Creates a new MiMaLib object to run analysis. */
  private def makeMima(cp: Classpath, log: Logging): MiMaLib = new MiMaLib(convCp(cp), log)

  /** Convert sbt's notion of a "Classpath" to nsc's notion of a "ClassPath". */
  private def convCp(cp: Classpath): ClassPath = aggregateClassPath(Attributed.data(cp))

  /** Runs MiMa and returns a two lists of potential binary incompatibilities,
      the first for backward compatibility checking, and the second for forward checking. */
  def runMima(prev: File, curr: File, cp: Classpath, dir: String, log: Logging): (List[Problem], List[Problem]) = {
    // MiMaLib collects problems to a mutable buffer, therefore we need a new instance every time
    def checkBC = makeMima(cp, log).collectProblems(prev, curr)
    def checkFC = makeMima(cp, log).collectProblems(curr, prev)
    dir match {
       case "backward" | "backwards" => (checkBC, Nil)
       case "forward" | "forwards"   => (Nil, checkFC)
       case "both"                   => (checkBC, checkFC)
       case _                        => (Nil, Nil)
    }
  }

  /** Reports binary compatibility errors for a module. */
  def reportModuleErrors(
      module: ModuleID,
      backward: List[Problem],
      forward: List[Problem],
      failOnProblem: Boolean,
      filters: Seq[ProblemFilter],
      backwardFilters: Map[String, Seq[ProblemFilter]],
      forwardFilters: Map[String, Seq[ProblemFilter]],
      log: Logging,
      projectName: String,
  ): Unit = {
    // filters * found is n-squared, it's fixable in principle by special-casing known
    // filter types or something, not worth it most likely...

    def isReported(map: Map[String, Seq[ProblemFilter]], classification: String) =
      ProblemReporting.isReported(module.revision, filters, map)(log, projectName, classification) _

    def pretty(affected: String)(p: Problem): String = {
      val desc = p.description(affected)
      val howToFilter = p.howToFilter.fold("")(s => s"\n   filter with: $s")
      s" * $desc$howToFilter"
    }

    val backErrors = backward.filter(isReported(backwardFilters, "current"))
    val forwErrors = forward.filter(isReported(forwardFilters, "other"))

    val count = backErrors.size + forwErrors.size
    val filteredCount = backward.size + forward.size - backErrors.size - forwErrors.size
    val filteredNote = if (filteredCount > 0) s" (filtered $filteredCount)" else ""
    log.info(s"$projectName: found $count potential binary incompatibilities while checking against $module $filteredNote")

    (backErrors.map(pretty("current")) ++ forwErrors.map(pretty("other"))).foreach { p =>
      if (failOnProblem) log.error(p)
      else log.warn(p)
    }

    if (failOnProblem && (backErrors.nonEmpty || forwErrors.nonEmpty)) {
      sys.error(s"$projectName: Binary compatibility check failed!")
    }
  }

  /** Resolves an artifact representing the previous abstract binary interface for testing. */
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
    optFile.getOrElse(sys.error(s"Could not resolve previous ABI: $m"))
  }

  def issueFiltersFromFiles(filtersDirectory: File, fileExtension: Regex, s: TaskStreams): Map[String, Seq[ProblemFilter]] = {
    if (filtersDirectory.exists) loadMimaIgnoredProblems(filtersDirectory, fileExtension, s.log)
    else Map.empty
  }

  def loadMimaIgnoredProblems(directory: File, fileExtension: Regex, logger: Logger): Map[String, Seq[ProblemFilter]] = {
    val ExclusionPattern = """ProblemFilters\.exclude\[([^\]]+)\]\("([^"]+)"\)""".r

    def findFiles(): Seq[(File, String)] = directory.listFiles().flatMap(f => fileExtension.findFirstIn(f.getName).map((f, _)))
    def parseFile(fileOrDir: File, extension: String): Either[Seq[Throwable], (String, Seq[ProblemFilter])] = {
      val version = fileOrDir.getName.dropRight(extension.length)

      def parseOneFile(file: File): Either[Seq[Throwable],Seq[ProblemFilter]] = {
        def parseLine(text: String, line: Int): Try[ProblemFilter] =
          Try {
            text match {
              case ExclusionPattern(className, target) => ProblemFilters.exclude(className, target)
              case x => throw new RuntimeException(s"Couldn't parse '$x'")
            }
          }.transform(Success(_), ex => Failure(ParsingException(file, line, ex)))

        val lines = try Source.fromFile(file).getLines().toVector catch {
          case NonFatal(t) => throw new RuntimeException(s"Couldn't load '$file'", t)
        }

        val (excludes, failures) =
          lines
            .filterNot { str => str.trim.isEmpty || str.trim.startsWith("#") }
            .zipWithIndex
            .map((parseLine _).tupled)
            .partition(_.isSuccess)

        if (failures.isEmpty) Right(excludes.map(_.get))
        else Left(failures.map(_.failed.get))
      }

      if (fileOrDir.isDirectory) {
        val allResults = fileOrDir.listFiles().toSeq.map(parseOneFile)
        val (mappings, failures) = allResults.partition(_.isRight)
        if (failures.isEmpty) Right(version -> mappings.flatMap(_.right.get))
        else Left(failures.flatMap(_.left.get))
      } else parseOneFile(fileOrDir).right.map(version -> _)
    }

    require(directory.exists(), s"Mima filter directory did not exist: ${directory.getAbsolutePath}")

    val (mappings, failures) = findFiles().map((parseFile _).tupled).partition(_.isRight)

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

  case class ParsingException(file: File, line: Int, ex: Throwable)
    extends RuntimeException(s"Error while parsing $file, line $line: ${ex.getMessage}", ex)
}
