package com.typesafe.tools.mima
package plugin

import sbt._

object MimaKeys {
  val failOnProblem = SettingKey[Boolean]("mima-fail-on-problem", "if true, fail the build on binary incompatibility detection.")

  @deprecated("Please use previousArtifacts which allows setting more than one previous artifact.", "0.1.8")
  val previousArtifact = SettingKey[Option[ModuleID]]("mima-previous-artifact", "Previous released artifact used to test binary compatibility.")

  val previousArtifacts = SettingKey[Set[ModuleID]]("mima-previous-artifacts", "Previous released artifacts used to test binary compatibility.")
  val previousClassfiles = TaskKey[Set[File]]("mima-previous-classfiles", "Directories or jars containing the previous class files used to test compatibility.")
  val currentClassfiles = TaskKey[File]("mima-current-classfiles", "Directory or jar containing the current class files used to test compatibility.")
  // TODO - Create a task to make a MiMaLib, is that a good idea?
  // findBinaryIssues returns *two* lists, one for backward and one for forward problems.
  val findBinaryIssues = TaskKey[List[(File, List[core.Problem], List[core.Problem])]]("mima-find-binary-issues", "A list of all binary incompatibilities between two files.")
  val reportBinaryIssues = TaskKey[Unit]("mima-report-binary-issues", "Logs all binary incompatibilities to the sbt console/logs.")

  val binaryIssueFilters = SettingKey[Seq[core.ProblemFilter]]("mima-binary-issue-filters", "A list of filters to apply to binary issues found. Applies both to backward and forward binary compatibility checking.")
  val backwardIssueFilters = SettingKey[Seq[core.ProblemFilter]]("mima-backward-issue-filters", "A list of filters to apply to binary issues found. These filters only apply to backward compatibility checking.")
  val forwardIssueFilters = SettingKey[Seq[core.ProblemFilter]]("mima-forward-issue-filters", "A list of filters to apply to binary issues found. These filters only apply to forward compatibility checking.")

  val checkDirection = SettingKey[String]("mima-check-direction", "Compatibility checking direction; default is \"backward\", but can also be \"forward\" or \"both\".")
}
