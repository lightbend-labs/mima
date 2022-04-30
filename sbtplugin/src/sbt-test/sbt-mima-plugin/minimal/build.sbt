import com.typesafe.tools.mima.core._

mimaPreviousArtifacts  := Set(organization.value %% name.value % "0.0.1-SNAPSHOT")
mimaBinaryIssueFilters := Seq(ProblemFilters.exclude[MissingMethodProblem]("A.bar"))
