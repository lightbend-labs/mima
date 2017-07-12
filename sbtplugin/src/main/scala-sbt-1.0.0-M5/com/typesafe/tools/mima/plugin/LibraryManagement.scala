package com.typesafe.tools.mima.plugin

import sbt._
import sbt.Keys.TaskStreams
import sbt.librarymanagement.IvyScala
import sbt.internal.librarymanagement._

object LibraryManagement {

  /** Resolves an artifact representing the previous abstract binary interface
    *  for testing.
    */
  def getPreviousArtifact(m: librarymanagement.ModuleID, ivyScala: Option[IvyScala], ivySbt: IvySbt, s: TaskStreams): File = {
    val moduleSettings = InlineConfiguration(
      validate = false, ivyScala,
      "dummy" % "test" % "version",
      ModuleInfo("dummy-test-project-for-resolving"),
      dependencies = Vector(m))
    val module = new ivySbt.Module(moduleSettings)

    val specialArtifactTypes = Artifact.DefaultSourceTypes union Artifact.DefaultDocTypes
    val artifactFilter = librarymanagement.ArtifactTypeFilter.forbid(specialArtifactTypes)

    val reportEither = sbt.alias.IvyActions.updateEither(
      module,
      librarymanagement.UpdateConfiguration(
        retrieve = None,
        missingOk = false,
        logging = librarymanagement.UpdateLogging.DownloadOnly,
        artifactFilter = artifactFilter,
        /*offline = false, These will be required for 1.0.0-M6
        frozen = false*/),
      UnresolvedWarningConfiguration(),
      LogicalClock.unknown,
      None,
      s.log)
    val optFile = (for {
      report <- reportEither.toOption
      config <- report.configurations.headOption
      module <- config.modules.headOption
      (artifact, file) <- module.artifacts.headOption
      if artifact.name == m.name
      if artifact.classifier.isEmpty
    } yield file).headOption
    optFile getOrElse sys.error("Could not resolve previous ABI: " + m)
  }
}

case class ModuleIdOps(m: ModuleID) {
  def withName(name: String) = m.withName(name)
}
