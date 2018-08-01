package com.typesafe.tools.mima.core

import scala.util.Try

import com.typesafe.tools.mima.core.util.log.Logging

object ProblemReporting {
  private[mima] def isReported(
      version: String,
      filters: Seq[ProblemFilter],
      versionedFilters: Map[String, Seq[ProblemFilter]]
  )(log: Logging, projectName: String)(problem: Problem): Boolean = {
    // version string "x.y.z" is converted to an Int tuple (x, y, z) for comparison
    val versionOrdering = Ordering[(Int, Int, Int)].on { version: String =>
      val ModuleVersion = """(\d+)\.?(\d+)?\.?(.*)?""".r
      val ModuleVersion(epoch, major, minor) = version
      val toNumeric = (revision: String) => Try(revision.replace("x", Short.MaxValue.toString).filter(_.isDigit).toInt).getOrElse(0)
      (toNumeric(epoch), toNumeric(major), toNumeric(minor))
    }

    (versionedFilters.collect {
      // get all filters that apply to given module version or any version after it
      case f @ (version2, versionFilters) if versionOrdering.gteq(version2, version) => versionFilters
    }.flatten ++ filters).forall { f =>
      if (f(problem)) {
        true
      } else {
        log.debugLog(projectName + ": filtered out: " + problem.description + "\n  filtered by: " + f)
        false
      }
    }
  }
}
