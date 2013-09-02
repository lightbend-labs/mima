package com.typesafe.tools.mima
package cli

import com.typesafe.tools.mima.core.{ProblemFilter, ProblemFilters}
import com.typesafe.config._
import scala.collection.JavaConverters._

object ProblemFiltersConfig {
  private val filterProblemsPath = "filter.problems"
  private val filterPackagesPath = "filter.packages"
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
    val individualFilter = for (problemConfig <- filters) yield {
      val problemClassName = problemConfig.getString(problemNameKey)
      val matchName = problemConfig.getString(matchNameKey)
      ProblemFilters.exclude(problemClassName, matchName)
    }
    val packages = config.getStringList(filterPackagesPath).asScala.toSeq
    val packageFilter = for (pack <- packages) yield {
      ProblemFilters.excludePackage(pack)
    }
    individualFilter ++ packageFilter
  }

  /**
   * Generates Config definition that filters passed collection of problems.
   */
  def problemsToProblemFilterConfig(problems: Seq[core.Problem]): Config = {
    val configValues = for {
      p <- problems
      matchName <- p.matchName
      problemName = p.getClass.getSimpleName
    } yield Map(problemNameKey -> problemName, matchNameKey -> matchName).asJava
    ConfigValueFactory.fromIterable(configValues.asJava).atPath(filterProblemsPath)
  }
}
