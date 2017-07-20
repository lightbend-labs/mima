package com.typesafe.tools.mima
package plugin

import sbt._
import sbt.Keys._
import sbt.compat._

/** Sbt plugin for using MiMa. */
object MimaPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] = mimaDefaultSettings

  object autoImport extends BaseMimaKeys
  import autoImport._

  /** Just configures MiMa to compare previous/current classfiles.*/
  def mimaReportSettings: Seq[Setting[_]] = Seq(
    mimaCheckDirection := "backward",
    mimaFiltersDirectory := (sourceDirectory in Compile).value / "mima-filters",
    mimaBinaryIssueFilters := Nil,
    mimaBackwardIssueFilters := SbtMima.issueFiltersFromFiles(mimaFiltersDirectory.value, "\\.(?:backward[s]?|both)\\.excludes".r, streams.value),
    mimaForwardIssueFilters := SbtMima.issueFiltersFromFiles(mimaFiltersDirectory.value, "\\.(?:forward[s]?|both)\\.excludes".r, streams.value),
    mimaFindBinaryIssues := {
      val taskStreams = streams.value
      val log = taskStreams.log
      val projectName = name.value
      val previousClassfiles =  mimaPreviousClassfiles.value
      val currentClassfiles = mimaCurrentClassfiles.value
      val cp = (fullClasspath in mimaFindBinaryIssues).value
      val checkDirection = mimaCheckDirection.value
      if (mimaPreviousClassfiles.value.isEmpty) {
        log.info(s"$projectName: previous-artifact not set, not analyzing binary compatibility")
        Map.empty
      }
      else {
        previousClassfiles.map {
          case (moduleId, file) =>
            val problems = SbtMima.runMima(file, currentClassfiles, cp, checkDirection, taskStreams)
            (moduleId, (problems._1, problems._2))
        }
      }
    },
    mimaReportBinaryIssues := {
      val taskStreams = streams.value
      val log = taskStreams.log
      val projectName = name.value
      val currentClassfiles = mimaCurrentClassfiles.value
      val cp = (fullClasspath in mimaFindBinaryIssues).value
      val checkDirection = mimaCheckDirection.value
      val failOnProblem = mimaFailOnProblem.value
      val binaryIssueFilters = mimaBinaryIssueFilters.value
      val backwardIssueFilters = mimaBackwardIssueFilters.value
      val forwardIssueFilters = mimaForwardIssueFilters.value
      if (mimaPreviousClassfiles.value.isEmpty) {
        log.info(s"$projectName: previous-artifact not set, not analyzing binary compatibility")
        Map.empty
      }
      else {
        mimaPreviousClassfiles.value.foreach {
          case (moduleId, file) =>
            val problems = SbtMima.runMima(
              file,
              currentClassfiles,
              cp,
              checkDirection,
              taskStreams
            )
            SbtMima.reportModuleErrors(
              moduleId,
              problems._1, problems._2,
              failOnProblem,
              binaryIssueFilters,
              backwardIssueFilters,
              forwardIssueFilters,
              taskStreams,
              projectName)
        }
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

      mimaPreviousArtifacts.value.map{ m =>
        // TODO - These should be better handled in sbt itself.
        // The cross version API is horribly intricately odd.
        CrossVersion(m, scalaModuleInfoV) match {
          case Some(f) => m withName f(m.name)
          case None => m
        }
      }.map{ id =>
        id -> SbtMima.getPreviousArtifact(id, ivy, taskStreams)
      }.toMap
    },
    fullClasspath in mimaFindBinaryIssues := (fullClasspath in Compile).value
  ) ++ mimaReportSettings

}
