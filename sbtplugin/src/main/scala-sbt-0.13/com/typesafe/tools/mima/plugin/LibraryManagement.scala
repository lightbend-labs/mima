package com.typesafe.tools.mima.plugin

import sbt._
import sbt.Keys.TaskStreams

object LibraryManagement {

  /** Resolves an artifact representing the previous abstract binary interface
    *  for testing.
    */
  def getPreviousArtifact(m: ModuleID, ivyScala: Option[IvyScala], ivy: IvySbt, s: TaskStreams): File = {
    val moduleSettings = InlineConfiguration(
      "dummy" % "test" % "version",
      ModuleInfo("dummy-test-project-for-resolving"),
      dependencies = Seq(m))
    val module = new ivy.Module(moduleSettings)
    val report = IvyActions.update(
      module,
      new UpdateConfiguration(
        retrieve = None,
        missingOk = false,
        logging = UpdateLogging.DownloadOnly),
      s.log)
    val optFile = (for {
      config <- report.configurations
      module <- config.modules
      (artifact, file) <- module.artifacts
      if artifact.name == m.name
      if artifact.classifier.isEmpty
    } yield file).headOption
    optFile getOrElse sys.error("Could not resolve previous ABI: " + m)
  }
}

case class ModuleIdOps(m: ModuleID) {
  def withName(name: String) = m.copy(name = name)
}
