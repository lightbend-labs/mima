package ssol.tools.mima
package plugin

import sbt._

object MimaKeys {
  val failOnProblem = SettingKey[Boolean]("mima-fail-on-problem", "if true, fail the build on binary incompatibility detection.")
  
  val previousArtifact = SettingKey[Option[ModuleID]]("mima-previous-artifact", "Previous released artifact used to test binary compatibility.")
  val previousClassfiles = TaskKey[File]("mima-previous-classfiles", "Directory or jar containing the previous class files used to test compatibility.")
  val currentClassfiles = TaskKey[File]("mima-current-classfiles", "Directory or jar containing the current class files used to test compatibility.")
  // TODO - Create a task to make a MiMaLib, is that a good idea?
  val findBinaryIssues = TaskKey[List[core.Problem]]("mima-find-binary-issues", "A list of all binary incompatibilities between two files.")
  val reportBinaryIssues = TaskKey[Unit]("mima-report-binary-issues", "Logs all binary incompatibilities to the sbt console/logs.")
  
}