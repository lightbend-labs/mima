package com.typesafe.tools.mima
package cli

import com.typesafe.tools.mima.core.{ProblemFilter, ProblemFilters}
import com.typesafe.config._
import scala.collection.JavaConverters._

object ProblemFiltersConfig {
  private val filterProblemsPath = "filter.problems"
  private val problemNameKey = "problemName"
  private val matchNameKey = "matchName"

  /**
   * Parses Config definition into sequence of problem filters.
   *
   * @exception ConfigException
   * @exception ClassNotFoundException if filter rule uses `problemName` that does not
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

  /**
   * Generates Config definition that filters passed collection of problems.
   */
  def problemsToProblemFilterConfig(problems: Seq[core.Problem]): Config = {
    val configValues = for {
      p <- problems
      matchName <- p.matchName
      val problemName = p.getClass.getSimpleName
    } yield Map(problemNameKey -> problemName, matchNameKey -> matchName).asJava
    ConfigValueFactory.fromIterable(configValues.asJava).atPath(filterProblemsPath)
  }
}
