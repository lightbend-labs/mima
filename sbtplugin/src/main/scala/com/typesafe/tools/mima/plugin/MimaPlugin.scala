package com.typesafe.tools.mima
package plugin

import sbt._
import sbt.Keys.{ fullClasspath, streams, classDirectory, ivySbt, name, ivyScala }

/** Sbt plugin for using MiMa. */
object MimaPlugin extends Plugin {
  import MimaKeys._
  /** Just configures MiMa to compare previous/current classfiles.*/
  def mimaReportSettings: Seq[Setting[_]] = Seq(
    binaryIssueFilters := Nil,
    findBinaryIssues := {
        previousClassfiles.value match {
          case Some(prev) =>
            SbtMima.runMima(
              prev, 
              currentClassfiles.value, 
              (fullClasspath in findBinaryIssues).value, 
              streams.value)
          case None =>
            streams.value.log.info(s"${name.value}: previous-artifact not set, not analyzing binary compatibility")
            Nil
        }
      },
    reportBinaryIssues <<= (findBinaryIssues, failOnProblem, binaryIssueFilters, streams, name) map SbtMima.reportErrors)
  /** Setup mima with default settings, applicable for most projects. */
  def mimaDefaultSettings: Seq[Setting[_]] = Seq(
    failOnProblem := true,
    previousArtifact := None,
    currentClassfiles := (classDirectory in Compile).value,
    previousClassfiles := {
      previousArtifact.value map { m => 
        // TODO - These should be better handled in sbt itself.
        // The cross version API is horribly intricately odd.
        CrossVersion(m, ivyScala.value) match {
          case Some(f) => m.copy(name = f(m.name))
          case None => m 
        }
      } map { id => 
        SbtMima.getPreviousArtifact(id, ivySbt.value, streams.value) 
      }
    },
    fullClasspath in findBinaryIssues := (fullClasspath in Compile).value
  ) ++ mimaReportSettings
}
