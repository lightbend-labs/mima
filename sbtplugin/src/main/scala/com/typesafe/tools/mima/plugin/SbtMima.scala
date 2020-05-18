package com.typesafe.tools.mima
package plugin

import java.io.File

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.lib.MiMaLib
import sbt._
import sbt.Keys.{ Classpath, TaskStreams }
import sbt.librarymanagement.{ UpdateLogging => _, _ }
import sbt.librarymanagement.ivy._
import sbt.internal.librarymanagement._

import scala.io.Source
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal
import scala.util.matching._

object SbtMima {
  /** Runs MiMa and returns a two lists of potential binary incompatibilities,
      the first for backward compatibility checking, and the second for forward checking. */
  def runMima(prev: File, curr: File, cp: Classpath, dir: String, scalaVersion: String, logger: Logger): (List[Problem], List[Problem]) = {
    val log = new SbtLogger(logger)
    sanityCheckScalaVersion(scalaVersion)
    val mimaLib = new MiMaLib(Attributed.data(cp), log)
    def checkBC = mimaLib.collectProblems(prev, curr)
    def checkFC = mimaLib.collectProblems(curr, prev)
    dir match {
       case "backward" | "backwards" => (checkBC, Nil)
       case "forward" | "forwards"   => (Nil, checkFC)
       case "both"                   => (checkBC, checkFC)
       case _                        => (Nil, Nil)
    }
  }

  private def sanityCheckScalaVersion(scalaVersion: String) = {
    scalaVersion.take(5) match {
      case "2.11." | "2.12." | "2.13." => () // ok
      case _ => throw new IllegalArgumentException(s"MiMa supports Scala 2.10-2.13, not $scalaVersion")
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
      logger: Logger,
      projectName: String,
  ): Unit = {
    // filters * found is n-squared, it's fixable in principle by special-casing known
    // filter types or something, not worth it most likely...
    val log = new SbtLogger(logger)

    def isReported(versionedFilters: Map[String, Seq[ProblemFilter]])(problem: Problem) = {
      ProblemReporting.isReported(module.revision, filters, versionedFilters)(problem)
    }

    def pretty(affected: String)(p: Problem): String = {
      val desc = p.description(affected)
      val howToFilter = p.howToFilter.fold("")(s => s"\n   filter with: $s")
      s" * $desc$howToFilter"
    }

    val backErrors = backward.filter(isReported(backwardFilters))
    val forwErrors = forward.filter(isReported(forwardFilters))

    val count = backErrors.size + forwErrors.size
    val doLog = if (count == 0) log.verbose(_) else if (failOnProblem) log.error(_) else log.warn(_)
    def logResult(msg: String) = doLog(s"$projectName: $msg")

    if (count == 0) {
      logResult(s"Binary compatibility check against $module passed.")
    } else {
      val filteredCount = backward.size + forward.size - count
      val filteredNote = if (filteredCount > 0) s" (filtered $filteredCount)" else ""
      val msg = s"Failed binary compatibility check against $module! Found $count potential problems$filteredNote"

      logResult(msg)
      for (p <- backErrors) doLog(pretty("current")(p))
      for (p <- forwErrors) doLog(pretty("other")(p))

      if (failOnProblem)
        sys.error(msg)
    }
  }

  /** Resolves an artifact representing the previous abstract binary interface for testing. */
  def getPreviousArtifact(m: ModuleID, ivy: IvySbt, s: TaskStreams): File = {
    val depRes = IvyDependencyResolution(ivy.configuration)
    val module = depRes.wrapDependencyInModule(m)
    val uc = UpdateConfiguration().withLogging(UpdateLogging.DownloadOnly)
    val uwc = UnresolvedWarningConfiguration()
    val report = depRes.update(module, uc, uwc, s.log).left.map(_.resolveException).toTry.get
    val jars = for {
      config <- report.configurations.iterator
      module <- config.modules
      (artifact, file) <- module.artifacts
      if artifact.name == m.name
      if artifact.classifier.isEmpty
    } yield file
    jars.toList match {
      case Nil             => sys.error(s"Could not resolve previous ABI: $m")
      case jar :: moreJars =>
        if (moreJars.nonEmpty)
          s.log.debug(s"Returning $jar and ignoring $moreJars for $m")
        jar
    }
  }

  def issueFiltersFromFiles(filtersDirectory: File, fileExtension: Regex, s: TaskStreams): Map[String, Seq[ProblemFilter]] = {
    val ExclusionPattern = """ProblemFilters\.exclude\[([^\]]+)\]\("([^"]+)"\)""".r
    val mappings = mutable.HashMap.empty[String, Seq[ProblemFilter]].withDefault(_ => new ListBuffer[ProblemFilter])
    val failures = new ListBuffer[String]

    def parseOneFile(file: File): Seq[ProblemFilter] = {
      val filters = new ListBuffer[ProblemFilter]
      val source = Source.fromFile(file)
      try {
        for {
          (rawText, line) <- source.getLines().zipWithIndex
          if !rawText.startsWith("#")
          text = rawText.trim
          if text != ""
        } {
          text match {
            case ExclusionPattern(className, target) =>
              try filters += ProblemFilters.exclude(className, target) catch {
                case NonFatal(t) => failures += s"Error while parsing $file, line $line: ${t.getMessage}"
              }
            case _ => failures += s"Couldn't parse $file, line $line: '$text'"
          }
        }
      } catch {
        case NonFatal(t) => failures += s"Couldn't load '$file': ${t.getMessage}"
      } finally source.close()
      filters
    }

    for {
      fileOrDir <- Option(filtersDirectory.listFiles()).getOrElse(Array.empty)
      extension <- fileExtension.findFirstIn(fileOrDir.getName)
      version = fileOrDir.getName.dropRight(extension.length)
      file <- Option(fileOrDir.listFiles()).getOrElse(Array(fileOrDir))
    } mappings(version) = mappings(version) ++ parseOneFile(file)

    if (failures.isEmpty) {
      mappings.toMap
    } else {
      failures.foreach(s.log.error(_))
      throw new RuntimeException(s"Loading Mima filters failed with ${failures.size} failures.")
    }
  }
}
