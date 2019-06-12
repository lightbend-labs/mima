package mimabuild

import sbt._
import sbt.librarymanagement.{ SemanticSelector, VersionNumber }
import sbt.Defaults.sbtPluginExtra
import sbt.Keys._
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._

object MimaSettings {
  val mimaPreviousVersion = "0.1.15"

  val mimaSettings = Def.settings (
    mimaPreviousArtifacts := {
      if (VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector(">=2.13"))) Set.empty else Set({
        val m = organization.value % moduleName.value % mimaPreviousVersion
        if (sbtPlugin.value)
          sbtPluginExtra(m, (sbtBinaryVersion in pluginCrossBuild).value, (scalaBinaryVersion in update).value)
        else
          m cross CrossVersion.binary
      })
    },
    mimaBinaryIssueFilters ++= Seq(
      // Removed because unused
      ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.core.buildinfo.BuildInfo"),
      ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.core.buildinfo.BuildInfo$"),

      // Add support for versions with less segments (#212)
      ProblemFilters.exclude[ReversedMissingMethodProblem]("com.typesafe.tools.mima.core.util.log.Logging.warn"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("com.typesafe.tools.mima.core.util.log.Logging.error"),

      // Removed because unused
      ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.lib.license.License"),
      ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.lib.license.License$"),

      // Removed because CLI dropped
      ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.MimaSpec"),
      ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.MimaSpec$"),
      ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.Main"),
      ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.Main$"),

      // Moved (to functional-tests) because otherwise unused
      ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.ProblemFiltersConfig"),
      ProblemFilters.exclude[MissingClassProblem]("com.typesafe.tools.mima.cli.ProblemFiltersConfig$"),

      // sbt-compat has been created to define these and has been added as a dependency of sbt-mima-plugin
      ProblemFilters.exclude[MissingClassProblem]("sbt.compat"),
      ProblemFilters.exclude[MissingClassProblem]("sbt.compat$"),
      ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.package"),
      ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.package$"),
      ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.package$UpdateConfigurationOps"),
      ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.package$UpdateConfigurationOps$"),
      ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.UpdateConfiguration"),
      ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.UpdateConfiguration$"),
      ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.DependencyResolution"),
      ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.ivy.IvyDependencyResolution"),
      ProblemFilters.exclude[MissingClassProblem]("sbt.librarymanagement.ivy.IvyDependencyResolution$"),

      // Add support for versions with less segments (#212)
      // All of these are MiMa sbtplugin internals and can be safely filtered
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.typesafe.tools.mima.plugin.SbtMima.runMima"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("com.typesafe.tools.mima.plugin.SbtMima.reportErrors"),
      ProblemFilters.exclude[IncompatibleMethTypeProblem]("com.typesafe.tools.mima.plugin.SbtMima.reportModuleErrors"),
    ),
  )
}
