package mimabuild

import sbt._
import sbt.librarymanagement.{ SemanticSelector, VersionNumber }
import sbt.Defaults.sbtPluginExtra
import sbt.Keys._
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.ProblemFilters.exclude
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._

object MimaSettings {
  val mimaPreviousVersion = "0.3.0"

  val mimaSettings = Def.settings (
    mimaPreviousArtifacts := {
      val scalaVersion       = VersionNumber(Keys.scalaVersion.value)
      val sbtBinaryVersion   = (Keys.sbtBinaryVersion in pluginCrossBuild).value
      val scalaBinaryVersion = (Keys.scalaBinaryVersion in pluginCrossBuild).value
      val moduleId0          = organization.value % moduleName.value % mimaPreviousVersion
      val moduleId =
        if (sbtPlugin.value)
          sbtPluginExtra(moduleId0, sbtBinaryVersion, scalaBinaryVersion)
        else
          moduleId0.cross(CrossVersion.binary)
      if (scalaVersion.matchesSemVer(SemanticSelector(">=2.13"))) Set.empty else Set(moduleId)
    },
    mimaBinaryIssueFilters ++= Seq(
      // The main public API is:
      // * com.typesafe.tools.mima.plugin.MimaPlugin
      // * com.typesafe.tools.mima.plugin.MimaKeys
      // * com.typesafe.tools.mima.core.ProblemFilters
      // * com.typesafe.tools.mima.core.*Problem
      // to a less degree (some re-implementors):
      // * com.typesafe.tools.mima.core.Config.setup
      // * com.typesafe.tools.mima.core.reporterClassPath
      // * com.typesafe.tools.mima.lib.MiMaLib.collectProblems
      exclude[MissingClassProblem]("*mima.plugin.BaseMimaKeys"),              // dropped (renamed to MimaKeys)
      exclude[MissingTypesProblem]("*mima.plugin.MimaKeys$"),                 // -> parent class rename
      exclude[MissingTypesProblem]("*mima.plugin.MimaPlugin$autoImport$"),    // -> parent class rename
      exclude[DirectMissingMethodProblem]("*mima.plugin.SbtMima.x"),          // a mistake
      exclude[DirectMissingMethodProblem]("*mima.plugin.SbtMima.isReported"), // dropped (package private)
    ),
  )
}
