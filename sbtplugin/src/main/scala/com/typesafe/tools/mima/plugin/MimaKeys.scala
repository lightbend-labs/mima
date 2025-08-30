package com.typesafe.tools.mima
package plugin

import com.typesafe.tools.mima.core.{ Problem, ProblemFilter }
import sbt.{ File, ModuleID, settingKey, taskKey } // no sbt._, to avoid 1.3+ only singleFileJsonFormatter
import sbt.librarymanagement.DependencyResolution

object MimaKeys extends MimaKeys

class MimaKeys {

  final val mimaPreviousArtifacts       = settingKey[Set[ModuleID]]("Previous released artifacts used to test binary compatibility.")

  @transient
  final val mimaReportBinaryIssues      = taskKey[Unit]("Logs all binary incompatibilities to the sbt console/logs.")
  final val mimaExcludeAnnotations      = settingKey[Seq[String]]("The fully-qualified class names of annotations that exclude problems")

  @transient
  final val mimaBinaryIssueFilters      = taskKey[Seq[ProblemFilter]]("Filters to apply to binary issues found. Applies both to backward and forward binary compatibility checking.")
  final val mimaFailOnProblem           = settingKey[Boolean]("if true, fail the build on binary incompatibility detection.")
  final val mimaFailOnNoPrevious        = settingKey[Boolean]("if true, fail the build if no previous artifacts are set.")
  final val mimaReportSignatureProblems = settingKey[Boolean]("if true, report `IncompatibleSignatureProblem`s.")

  @transient
  final val mimaDependencyResolution = taskKey[DependencyResolution]("DependencyResolution to use to fetch previous artifacts.")

  @transient
  final val mimaPreviousClassfiles   = taskKey[Map[ModuleID, File]]("Directories or jars containing the previous class files used to test compatibility with a given module.")

  @transient
  final val mimaCurrentClassfiles    = taskKey[File]("Directory or jar containing the current class files used to test compatibility.")

  @transient
  final val mimaCheckDirection       = settingKey[String]("Compatibility checking direction; default is \"backward\", but can also be \"forward\" or \"both\".")

  @transient
  final val mimaFindBinaryIssues     = taskKey[Map[ModuleID, (List[Problem], List[Problem])]]("All backward and forward binary incompatibilities between a given module and current project.")

  @transient
  final val mimaBackwardIssueFilters = taskKey[Map[String, Seq[ProblemFilter]]]("Filters to apply to binary issues found grouped by version of a module checked against. These filters only apply to backward compatibility checking.")

  @transient
  final val mimaForwardIssueFilters  = taskKey[Map[String, Seq[ProblemFilter]]]("Filters to apply to binary issues found grouped by version of a module checked against. These filters only apply to forward compatibility checking.")

  @transient
  final val mimaFiltersDirectory     = settingKey[File]("Directory containing issue filters.")

}
