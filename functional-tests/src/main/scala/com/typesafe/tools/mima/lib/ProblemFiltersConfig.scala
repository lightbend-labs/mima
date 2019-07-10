package com.typesafe.tools.mima
package lib

import com.typesafe.tools.mima.core.{ProblemFilter, ProblemFilters}
import com.typesafe.config._
import scala.jdk.CollectionConverters._

object ProblemFiltersConfig {
  private val filterProblemsPath = "filter.problems"
  private val problemNameKey = "problemName"
  private val matchNameKey = "matchName"

  /**
   * Parses Config definition into sequence of problem filters.
   *
   * @throws ConfigException
   * @throws ClassNotFoundException if filter rule uses `problemName` that does not
   *                                   correspond to existing Problem class.
   */
  def parseProblemFilters(config: Config): Seq[ProblemFilter] = {
    val filters = config.getConfigList(filterProblemsPath).asScala.toSeq
    for (problemConfig <- filters) yield {
      val problemClassName = problemConfig.getString(problemNameKey)
      val matchName = problemConfig.getString(matchNameKey)
      ProblemFilters.exclude(problemClassName, matchName)
    }
  }
}
