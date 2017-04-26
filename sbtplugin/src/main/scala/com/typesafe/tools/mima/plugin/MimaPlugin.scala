package com.typesafe.tools.mima
package plugin

import sbt._
import sbt.Keys.{ fullClasspath, streams, classDirectory, ivySbt, name, ivyScala }

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
    mimaBinaryIssueFilters := Nil,
    mimaBackwardIssueFilters := Map.empty,
    mimaForwardIssueFilters := Map.empty,
    mimaFindBinaryIssues := {
      if (mimaPreviousClassfiles.value.isEmpty) {
        streams.value.log.info(s"${name.value}: previous-artifact not set, not analyzing binary compatibility")
        Map.empty
      }
      else {
        mimaPreviousClassfiles.value.map {
          case (moduleId, file) =>
            val problems = SbtMima.runMima(
              file,
              mimaCurrentClassfiles.value,
              (fullClasspath in mimaFindBinaryIssues).value,
              mimaCheckDirection.value,
              streams.value
            )
            (moduleId, (problems._1, problems._2))
        }
      }
    },
    mimaReportBinaryIssues := {
      if (mimaPreviousClassfiles.value.isEmpty) {
        streams.value.log.info(s"${name.value}: previous-artifact not set, not analyzing binary compatibility")
        Map.empty
      }
      else {
        mimaPreviousClassfiles.value.foreach {
          case (moduleId, file) =>
            val problems = SbtMima.runMima(
              file,
              mimaCurrentClassfiles.value,
              (fullClasspath in mimaFindBinaryIssues).value,
              mimaCheckDirection.value,
              streams.value
            )
            SbtMima.reportModuleErrors(
              moduleId,
              problems._1, problems._2,
              mimaFailOnProblem.value,
              mimaBinaryIssueFilters.value,
              mimaBackwardIssueFilters.value,
              mimaForwardIssueFilters.value,
              streams.value,
              name.value)
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
      mimaPreviousArtifacts.value.map{ m =>
        // TODO - These should be better handled in sbt itself.
        // The cross version API is horribly intricately odd.
        CrossVersion(m, ivyScala.value) match {
          case Some(f) => m.copy(name = f(m.name))
          case None => m
        }
      }.map{ id =>
        id -> SbtMima.getPreviousArtifact(id, ivySbt.value, streams.value)
      }.toMap
    },
    fullClasspath in mimaFindBinaryIssues := (fullClasspath in Compile).value
  ) ++ mimaReportSettings
}
