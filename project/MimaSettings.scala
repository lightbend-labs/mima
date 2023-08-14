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
  val mimaPreviousVersion = "1.1.3"

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

      ProblemFilters.exclude[DirectMissingMethodProblem]("com.typesafe.tools.mima.core.TastyUnpickler#DefDef.copy"),
      ProblemFilters.exclude[IncompatibleResultTypeProblem]("com.typesafe.tools.mima.core.TastyUnpickler#DefDef.copy$default$3"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("com.typesafe.tools.mima.core.TastyUnpickler#DefDef.this"),
      ProblemFilters.exclude[MissingTypesProblem]("com.typesafe.tools.mima.core.TastyUnpickler$DefDef$"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("com.typesafe.tools.mima.core.TastyUnpickler#DefDef.<init>$default$3"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("com.typesafe.tools.mima.core.TastyUnpickler#DefDef.apply"),
      ProblemFilters.exclude[DirectMissingMethodProblem]("com.typesafe.tools.mima.core.TastyUnpickler#DefDef.apply$default$3"),
      ProblemFilters.exclude[IncompatibleSignatureProblem]("com.typesafe.tools.mima.core.TastyUnpickler#DefDef.unapply"),
    ),
  )
}
