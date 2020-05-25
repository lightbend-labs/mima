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
  val mimaPreviousVersion = "0.7.0"

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
      exclude[Problem]("*mima.core.ClassfileParser*"),
      exclude[Problem]("*mima.core.*Info*"),
      exclude[Problem]("*mima.core.Config*"),
      exclude[Problem]("*mima.core.Definitions*"),
      exclude[Problem]("*mima.core.DeprecatedPathApis*"),
      exclude[Problem]("*mima.core.package.*"),
      exclude[Problem]("*mima.core.util*"),
      exclude[Problem]("*mima.lib.MiMaLib.*"),
      exclude[Problem]("*mima.plugin.SbtLogger*"),
      exclude[Problem]("*mima.plugin.SbtMima.*"),
      exclude[Problem]("scala.tools.nsc.mima*"),
      exclude[DirectMissingMethodProblem]("*mima.lib.analyze.method.MethodChecker.hasMatchingCtorSig"),
    ),
  )
}
