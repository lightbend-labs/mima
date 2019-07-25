package mimabuild

import sbt._
import sbt.librarymanagement.{ SemanticSelector, VersionNumber }
import sbt.Defaults.sbtPluginExtra
import sbt.Keys._
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.ProblemFilters.exclude
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._

object MimaSettings {
  // clear out mimaBinaryIssueFilters when changing this
  val mimaPreviousVersion = "0.5.0"

  private val isScala213OrLater =
    Def.setting(VersionNumber(scalaVersion.value).matchesSemVer(SemanticSelector(">=2.13")))

  val mimaSettings = Def.settings (
    mimaPreviousArtifacts := {
      val sbtBinaryVersion   = (Keys.sbtBinaryVersion in pluginCrossBuild).value
      val scalaBinaryVersion = (Keys.scalaBinaryVersion in pluginCrossBuild).value
      val moduleId0          = organization.value % moduleName.value % mimaPreviousVersion
      val moduleId =
        if (sbtPlugin.value)
          sbtPluginExtra(moduleId0, sbtBinaryVersion, scalaBinaryVersion)
        else
          moduleId0.cross(CrossVersion.binary)
      if (isScala213OrLater.value) Set.empty else Set(moduleId)
    },
    ThisBuild / mimaFailOnNoPrevious := !isScala213OrLater.value,
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

      // Through GitHub search this looks totally unused
      // Dropped to split the settings into global/build/projectSettings
      ProblemFilters.exclude[DirectMissingMethodProblem]("*mima.plugin.MimaPlugin.mimaReportSettings"),

      // Dropped deprecated method
      ProblemFilters.exclude[DirectMissingMethodProblem]("*mima.core.ProblemFilters.excludePackage"),
      // Changes to the members of the *Problem classes
      ProblemFilters.exclude[MemberProblem]("*mima.core.*Problem.*"),
      // *Problem classes made final
      ProblemFilters.exclude[FinalClassProblem]("*mima.core.*Problem"),
      // Dropped unused methods on classes in the hierarchy of the *Problem classes
      ProblemFilters.exclude[DirectMissingMethodProblem]("*mima.core.ProblemRef.*"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("*mima.core.TemplateRef.*"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("*mima.core.MemberRef.*"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("*mima.core.TemplateProblem.*"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("*mima.core.MemberProblem.*"),
      // Changes within the internal parsing code
      ProblemFilters.exclude[Problem]("*mima.core.BytesReader*"),
      ProblemFilters.exclude[Problem]("*mima.core.BufferReader*"),
      ProblemFilters.exclude[Problem]("*mima.core.ClassfileParser*"),
      ProblemFilters.exclude[Problem]("*mima.core.ClientClassfileParser"),
      ProblemFilters.exclude[Problem]("*mima.core.LibClassfileParser"),
      ProblemFilters.exclude[Problem]("*mima.core.UTF8Codec*"),
      // Dropped dead code internal to types
      ProblemFilters.exclude[Problem]("*mima.core.Type*"),
      ProblemFilters.exclude[Problem]("*mima.core.ClassType*"),
      ProblemFilters.exclude[Problem]("*mima.core.ArrayType*"),
      ProblemFilters.exclude[Problem]("*mima.core.ValueType*"),
      // Dropped dead code internal to the info classes
      ProblemFilters.exclude[Problem]("*mima.core.Definitions*"),
      ProblemFilters.exclude[Problem]("*mima.core.ClassInfo*"),
      ProblemFilters.exclude[Problem]("*mima.core.MemberInfo*"),
      ProblemFilters.exclude[Problem]("*mima.core.NoClass*"),
      ProblemFilters.exclude[Problem]("*mima.core.Members*"),
      ProblemFilters.exclude[Problem]("*mima.core.NoMembers*"),
      ProblemFilters.exclude[Problem]("*mima.core.WithLocalModifier*"),
      // Changes in internal, analyze code
      ProblemFilters.exclude[Problem]("*mima.lib.analyze*"),
      // Changes in internal code
      ProblemFilters.exclude[Problem]("*mima.core.Reference*"),
      ProblemFilters.exclude[Problem]("*mima.core.util.*"),
    ),
  )
}
