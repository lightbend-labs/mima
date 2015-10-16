package com.typesafe.tools.mima
package plugin

import sbt._

object MimaKeys extends BaseMimaKeys

// TODO - Create a task to make a MiMaLib, is that a good idea?
class BaseMimaKeys {
  @deprecated("Please use mimaPreviousArtifacts which allows setting more than one previous artifact.", "0.1.8")
  final val previousArtifact = SettingKey[Option[ModuleID]]("mima-previous-artifact", "Previous released artifact used to test binary compatibility.")

  final val mimaFailOnProblem      = settingKey[Boolean]("if true, fail the build on binary incompatibility detection.")
  final val mimaPreviousArtifacts  = settingKey[Set[ModuleID]]("Previous released artifacts used to test binary compatibility.")
  final val mimaPreviousClassfiles = taskKey[Set[File]]("Directories or jars containing the previous class files used to test compatibility.")
  final val mimaCurrentClassfiles  = taskKey[File]("Directory or jar containing the current class files used to test compatibility.")
  final val mimaFindBinaryIssues   = taskKey[List[(File, List[core.Problem], List[core.Problem])]]("A list of all backward and forward binary incompatibilities between two files.")
  final val mimaReportBinaryIssues = taskKey[Unit]("Logs all binary incompatibilities to the sbt console/logs.")

  final val mimaBinaryIssueFilters   = settingKey[Seq[core.ProblemFilter]]("A list of filters to apply to binary issues found. Applies both to backward and forward binary compatibility checking.")
  final val mimaBackwardIssueFilters = settingKey[Seq[core.ProblemFilter]]("A list of filters to apply to binary issues found. These filters only apply to backward compatibility checking.")
  final val mimaForwardIssueFilters  = settingKey[Seq[core.ProblemFilter]]("A list of filters to apply to binary issues found. These filters only apply to forward compatibility checking.")

  final val mimaCheckDirection = settingKey[String]("Compatibility checking direction; default is \"backward\", but can also be \"forward\" or \"both\".")

  @deprecated("Use mimaFailOnProblem",      "0.1.9") final val failOnProblem      = mimaFailOnProblem
  @deprecated("Use mimaPreviousArtifacts",  "0.1.9") final val previousArtifacts  = mimaPreviousArtifacts
  @deprecated("Use mimaPreviousClassfiles", "0.1.9") final val previousClassfiles = mimaPreviousClassfiles
  @deprecated("Use mimaCurrentClassfiles",  "0.1.9") final val currentClassfiles  = mimaCurrentClassfiles
  @deprecated("Use mimaFindBinaryIssues",   "0.1.9") final val findBinaryIssues   = mimaFindBinaryIssues
  @deprecated("Use mimaReportBinaryIssues", "0.1.9") final val reportBinaryIssues = mimaReportBinaryIssues
  @deprecated("Use mimaBinaryIssueFilters", "0.1.9") final val binaryIssueFilters = mimaBinaryIssueFilters
}
