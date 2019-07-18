import com.typesafe.tools.mima.core._

mimaPreviousArtifacts := Set(organization.value %% name.value % "0.0.1-SNAPSHOT")
mimaBackwardIssueFilters := Map(
  "0.0.1-SNAPSHOT" -> Seq(ProblemFilters.exclude[MissingMethodProblem]("A.bar"))
)
