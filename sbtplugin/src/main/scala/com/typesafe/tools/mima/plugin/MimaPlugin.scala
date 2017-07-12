package com.typesafe.tools.mima
package plugin

import sbt._
import sbt.Keys._

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
      val streamsValue = streams.value
      val nameValue = name.value

      val issues = mimaPreviousClassfiles.value.map {
        case (moduleId, file) =>
          val problems = SbtMima.runMima(
            file,
            mimaCurrentClassfiles.value,
            (fullClasspath in mimaFindBinaryIssues).value,
            mimaCheckDirection.value,
            streamsValue
          )
          (moduleId, (problems._1, problems._2))
      }

      if (issues.isEmpty) {
        streamsValue.log.info(s"$nameValue: previous-artifact not set, not analyzing binary compatibility")
      }

      issues
    },
    mimaReportBinaryIssues := {
      val streamsValue = streams.value
      val nameValue = name.value

      mimaPreviousClassfiles.value.foreach {
        case (moduleId, file) =>
          val problems = SbtMima.runMima(
            file,
            mimaCurrentClassfiles.value,
            (fullClasspath in mimaFindBinaryIssues).value,
            mimaCheckDirection.value,
            streamsValue
          )
          SbtMima.reportModuleErrors(
            moduleId,
            problems._1, problems._2,
            mimaFailOnProblem.value,
            mimaBinaryIssueFilters.value,
            mimaBackwardIssueFilters.value,
            mimaForwardIssueFilters.value,
            streamsValue,
            name.value)
      }

      if (mimaPreviousClassfiles.value.isEmpty) {
        streamsValue.log.info(s"$nameValue: previous-artifact not set, not analyzing binary compatibility")
      }
    }
  )

  /** Setup mima with default settings, applicable for most projects. */
  def mimaDefaultSettings: Seq[Setting[_]] = Seq(
    mimaFailOnProblem := true,
    mimaPreviousArtifacts := Set.empty,
    mimaCurrentClassfiles := (classDirectory in Compile).value,
    mimaPreviousClassfiles := {
      val ivyScalaValue = ivyScala.value
      val ivySbtValue = ivySbt.value
      val streamsValue = streams.value

      mimaPreviousArtifacts.value.map { m =>
        // TODO - These should be better handled in sbt itself.
        // The cross version API is horribly intricately odd.
        CrossVersion(m, ivyScalaValue) match {
          case Some(f) => ModuleIdOps(m).withName(f(m.name))
          case None => m
        }
      }.map{ id =>
        id -> LibraryManagement.getPreviousArtifact(id, ivyScalaValue, ivySbtValue, streamsValue)
      }.toMap
    },
    fullClasspath in mimaFindBinaryIssues := (fullClasspath in Compile).value
  ) ++ mimaReportSettings

}
