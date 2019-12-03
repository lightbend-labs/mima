package com.typesafe.tools.mima.core

import scala.util.Try

object ProblemReporting {
  private[mima] def isReported(
      version: String,
      filters: Seq[ProblemFilter],
      versionedFilters: Map[String, Seq[ProblemFilter]]
  )(problem: Problem): Boolean = {
    // version string "x.y.z" is converted to an Int tuple (x, y, z) for comparison
    val versionMatchingFilters = versionedFilters
      // get all filters that apply to given module version or any version after it
      .collect { case (version, filters) if versionOrdering.gteq(version, version) => filters }
      .flatten

    (versionMatchingFilters.iterator ++ filters).forall(filter => filter(problem))
  }

  private[mima] val versionOrdering: Ordering[String] = {
    val VersionRegex = """(\d+)\.?(\d+)?\.?(.*)?""".r
    val versionPartToInt = (versionPart: String) =>
      Try(versionPart.replace("x", Short.MaxValue.toString).filter(_.isDigit).toInt).getOrElse(0)

    Ordering[(Int, Int, Int)].on[String] { case VersionRegex(x, y, z) =>
      (versionPartToInt(x), versionPartToInt(y), versionPartToInt(z))
    }
  }
}
