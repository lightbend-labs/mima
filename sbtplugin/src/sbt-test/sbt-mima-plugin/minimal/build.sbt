import com.typesafe.tools.mima.core._

mimaPreviousArtifacts := Set(organization.value %% name.value % "0.0.1-SNAPSHOT")

val issueFilters = SettingKey[Seq[ProblemFilter]]("")
issueFilters := Seq(
  ProblemFilters.exclude[MissingMethodProblem]("A.bar")
)
