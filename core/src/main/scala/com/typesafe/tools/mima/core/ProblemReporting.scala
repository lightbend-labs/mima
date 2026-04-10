package com.typesafe.tools.mima.core

import scala.util.Try

private[mima] object ProblemReporting {
  val versionOrdering: Ordering[String] = {
    // version string "x.y.z" is converted to a Long tuple (x, y, z) for comparison
    val VersionRegex = """(\d+)\.?(\d+)?\.?(.*)?""".r
    def long(versionPart: String) =
      Try(versionPart.replace("x", Long.MaxValue.toString).filter(_.isDigit).toLong).getOrElse(0L)
    Ordering[(Long, Long, Long)].on[String] {
      case VersionRegex(x, y, z) => (long(x), long(y), long(z))
      case bad => throw new IllegalArgumentException(bad)
    }
  }

  def isReported(
      version: String,
      filters: Seq[ProblemFilter],
      versionedFilters: Map[String, Seq[ProblemFilter]]
  )(problem: Problem): Boolean = {
    val versionMatchingFilters = versionedFilters
      // get all filters that apply to given module version or any version after it
      .collect { case (version2, filters) if versionOrdering.gteq(version2, version) => filters }
      .flatten

    (versionMatchingFilters.iterator ++ filters).forall(filter => filter(problem))
  }
}
