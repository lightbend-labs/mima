import com.typesafe.tools.mima.core._

mimaBinaryIssueFilters := Seq(ProblemFilters.exclude[MissingMethodProblem]("A.bar"))
