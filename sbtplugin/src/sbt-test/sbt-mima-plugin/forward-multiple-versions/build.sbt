import com.typesafe.tools.mima.core._

mimaCheckDirection := "forward"
mimaPreviousArtifacts := Set("0.0.1-SNAPSHOT", "0.0.2-SNAPSHOT") map { v => organization.value %% name.value % v }

val issueFilters = SettingKey[Map[String, Seq[ProblemFilter]]]("")
issueFilters := Map(
  "0.0.1-SNAPSHOT" -> Seq(ProblemFilters.exclude[MissingMethodProblem]("A.bar")),
  "0.0.2-SNAPSHOT" -> Seq(ProblemFilters.exclude[MissingMethodProblem]("A.fooBar"))
)
