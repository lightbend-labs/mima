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
    forwardIssueFilters := Nil,
    backwardIssueFilters := Nil,
    findBinaryIssues := {
      if (previousClassfiles.value.isEmpty) {
        streams.value.log.info(s"${name.value}: previous-artifact not set, not analyzing binary compatibility")
        List.empty[(File, List[core.Problem], List[core.Problem])]
      }
      else {
        previousClassfiles.value.map { previous =>
          val problems = SbtMima.runMima(
            previous,
            currentClassfiles.value,
            (fullClasspath in findBinaryIssues).value,
            checkDirection.value,
            streams.value
          )
          (previous, problems._1, problems._2)
        }(scala.collection.breakOut)
      }
    },
    reportBinaryIssues <<= (findBinaryIssues, failOnProblem, binaryIssueFilters, backwardIssueFilters,
                            forwardIssueFilters, streams, name) map { (find, fail, bin, back, forw, s, n) =>
                              SbtMima.reportErrors(find, fail, bin ++ back, bin ++ forw, s, n)
                           })
  /** Setup mima with default settings, applicable for most projects. */
  def mimaDefaultSettings: Seq[Setting[_]] = Seq(
    failOnProblem := true,
    previousArtifact := None,
    previousArtifacts := Set.empty,
    currentClassfiles := (classDirectory in Compile).value,
    previousClassfiles := {
      (previousArtifacts.value ++ previousArtifact.value) map { m =>
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
