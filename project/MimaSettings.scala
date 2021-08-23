package mimabuild

import sbt._
import sbt.librarymanagement.{ SemanticSelector, VersionNumber }
import sbt.Classpaths.pluginProjectID
import sbt.Keys._
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.ProblemFilters.exclude
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._

object MimaSettings {
  // clear out mimaBinaryIssueFilters when changing this
  val mimaPreviousVersion = "0.9.2"

  val mimaSettings = Def.settings (
    mimaPreviousArtifacts := Set(pluginProjectID.value.withRevision(mimaPreviousVersion)
      .withExplicitArtifacts(Vector()) // defaultProjectID uses artifacts.value which breaks it =/
    ),
    mimaReportSignatureProblems := true,
    mimaBinaryIssueFilters ++= Seq(
      // The main public API is:
      // * com.typesafe.tools.mima.plugin.MimaPlugin
      // * com.typesafe.tools.mima.plugin.MimaKeys
      // * com.typesafe.tools.mima.core.ProblemFilters
      // * com.typesafe.tools.mima.core.*Problem
      // * com.typesafe.tools.mima.core.util.log.Logging
      exclude[Problem]("*mima.core.Pickle*"),
      exclude[Problem]("*mima.core.*pickle*"),
      exclude[Problem]("*mima.lib.MiMaLib.*"),
      exclude[Problem]("*mima.lib.analyze.Analyzer.*"),
      exclude[Problem]("*mima.plugin.SbtMima.*"),
    ),
  )
}
