package com.typesafe.tools.mima.core

import org.scalatest._
import org.scalatest.prop.TableDrivenPropertyChecks

class ProblemFiltersSpec extends WordSpec with TableDrivenPropertyChecks with Matchers {
  import ProblemFiltersSpec._

  val filters = Table(
    ("filter", "problem", "realProblem"),
    (ProblemFilters.exclude[Problem]("impl.Http"), problem("impl.Http"),  false),
    (ProblemFilters.exclude[Problem]("impl.Http"), problem("impl.Http2"), true),
    (ProblemFilters.exclude[Problem]("impl.*"),    problem("impl.Http"),  false),
    (ProblemFilters.exclude[Problem]("impl.*"),    problem("impl2.Http"), true),
    (ProblemFilters.exclude[Problem]("a$Impl*"),   problem("a$Impl$B"),   false),
    (ProblemFilters.exclude[Problem]("a$Impl*"),   problem("a2$Impl$B"),  true)
  )

  "problem filters" should {
    "filter problems" in {
      forAll (filters) { (filter, problem, realProblem) =>
        filter(problem) shouldBe realProblem
      }
    }
  }

}

object ProblemFiltersSpec {
  def problem(name: String) = new TemplateProblem(NoClass) {
    override def description: (String) => String = ???
    override def matchName = name
  }
}
