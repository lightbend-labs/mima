package com.typesafe.tools.mima
package plugin

import sbt._
import sbt.Keys.{ fullClasspath, streams, classDirectory, ivySbt, name }

/** Sbt plugin for using MiMa. */
object MimaPlugin extends Plugin {
  import MimaKeys._
  /** Just configures MiMa to compare previous/current classfiles.*/
  def mimaReportSettings: Seq[Setting[_]] = Seq(
    findBinaryIssues <<= (previousClassfiles, currentClassfiles,
      fullClasspath in findBinaryIssues, streams, name) map { (prevOption, curr, cp, s, name) =>
        prevOption match {
          case Some(prev) =>
            SbtMima.runMima(prev, curr, cp, s)
          case None =>
            s.log.info(name + ": previous-artifact not set, not analyzing binary compatibility")
            Nil
        }
      },
    reportBinaryIssues <<= (findBinaryIssues, failOnProblem, streams, name) map SbtMima.reportErrors)
  /** Setup mima with default settings, applicable for most projects. */
  def mimaDefaultSettings: Seq[Setting[_]] = Seq(
    failOnProblem := true,
    previousArtifact := None,
    currentClassfiles <<= classDirectory in Compile map identity,
    previousClassfiles <<= (ivySbt, previousArtifact, streams) map { (i, optArt, s) =>
      optArt map { art => SbtMima.getPreviousArtifact(art, i, s) }
    },
    fullClasspath in findBinaryIssues <<= fullClasspath in Compile) ++ mimaReportSettings
}
