package com.typesafe.tools.mima
package plugin

import sbt._
import sbt.Keys.{fullClasspath, streams, classDirectory, ivySbt}

/** Sbt plugin for using MiMa. */
object MimaPlugin extends Plugin {
  import MimaKeys._
  /** Just configures MiMa to compare previous/current classfiles.*/
  def mimaReportSettings: Seq[Setting[_]] = Seq(
    findBinaryIssues <<= (previousClassfiles, currentClassfiles, 
                          fullClasspath in findBinaryIssues, streams
                          ) map SbtMima.runMima,
    reportBinaryIssues <<= (findBinaryIssues, failOnProblem, streams) map SbtMima.reportErrors
  )
  /** Setup mima with default settings, applicable for most projects. */
  def mimaDefaultSettings: Seq[Setting[_]] = Seq(
    failOnProblem := true,
    previousArtifact := None,
    currentClassfiles <<= classDirectory in Compile map identity,
    previousClassfiles <<= (ivySbt, previousArtifact, streams) map { (i, optArt, s) =>
      val art = optArt getOrElse sys.error("No previous-artifact defined.  Cannot check binary compatibility.")
      SbtMima.getPreviousArttifact(art, i, s)
    },
    fullClasspath in findBinaryIssues <<= fullClasspath in Compile
  ) ++ mimaReportSettings
}