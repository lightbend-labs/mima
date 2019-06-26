package com.typesafe.tools.mima
package plugin

import sbt._
import sbt.Keys._

/** Sbt plugin for using MiMa. */
object MimaPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport extends BaseMimaKeys
  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    mimaFailOnNoPrevious := true,
  )

  override def projectSettings: Seq[Def.Setting[_]] = mimaDefaultSettings

  /** Just configures MiMa to compare previous/current classfiles.*/
  def mimaReportSettings: Seq[Setting[_]] = Seq(
    mimaCheckDirection := "backward",
    mimaFiltersDirectory := (sourceDirectory in Compile).value / "mima-filters",
    mimaBinaryIssueFilters := Nil,
    mimaBackwardIssueFilters := SbtMima.issueFiltersFromFiles(mimaFiltersDirectory.value, "\\.(?:backward[s]?|both)\\.excludes".r, streams.value),
    mimaForwardIssueFilters := SbtMima.issueFiltersFromFiles(mimaFiltersDirectory.value, "\\.(?:forward[s]?|both)\\.excludes".r, streams.value),
    mimaFindBinaryIssues := binaryIssuesIterator.value.toMap,
    mimaReportBinaryIssues := {
      val log = new SbtLogger(streams.value)
      val projectName = name.value
      val failOnProblem = mimaFailOnProblem.value
      val binaryIssueFilters = mimaBinaryIssueFilters.value
      val backwardIssueFilters = mimaBackwardIssueFilters.value
      val forwardIssueFilters = mimaForwardIssueFilters.value
      binaryIssuesIterator.value.foreach { case (moduleId, problems) =>
        SbtMima.reportModuleErrors(
          moduleId,
          problems._1,
          problems._2,
          failOnProblem,
          binaryIssueFilters,
          backwardIssueFilters,
          forwardIssueFilters,
          log,
          projectName,
        )
      }
    }
  )

  /** Setup mima with default settings, applicable for most projects. */
  def mimaDefaultSettings: Seq[Setting[_]] = Seq(
    mimaFailOnProblem := true,
    mimaPreviousArtifacts := Set.empty,
    mimaCurrentClassfiles := (classDirectory in Compile).value,
    mimaPreviousClassfiles := {
      val scalaModuleInfoV = scalaModuleInfo.value
      val ivy = ivySbt.value
      val taskStreams = streams.value

      mimaPreviousArtifacts.value.iterator.map { m =>
        val nameMod = CrossVersion(m, scalaModuleInfoV).getOrElse(idFun)
        val id = m.withName(nameMod(Project.normalizeModuleID(m.name)))
        id -> SbtMima.getPreviousArtifact(id, ivy, taskStreams)
      }.toMap
    },
    fullClasspath in mimaFindBinaryIssues := (fullClasspath in Compile).value
  ) ++ mimaReportSettings

  // Allows reuse between mimaFindBinaryIssues and mimaReportBinaryIssues
  // without blowing up the Akka build's heap
  private def binaryIssuesIterator = Def.task {
    val log = new SbtLogger(streams.value)
    val projectName = name.value
    val previousClassfiles = mimaPreviousClassfiles.value
    val failOnNoPrevious = mimaFailOnNoPrevious.value
    val currentClassfiles = mimaCurrentClassfiles.value
    val cp = (fullClasspath in mimaFindBinaryIssues).value
    val checkDirection = mimaCheckDirection.value
    if (previousClassfiles.isEmpty) {
      val msg = s"$projectName: mimaPreviousArtifacts not set, not analyzing binary compatibility"
      if (failOnNoPrevious) sys.error(msg)
      log.info(msg)
      Iterator.empty
    }
    else {
      previousClassfiles.iterator.map { case (moduleId, file) =>
        val problems = SbtMima.runMima(file, currentClassfiles, cp, checkDirection, log)
        (moduleId, (problems._1, problems._2))
      }
    }
  }
}
