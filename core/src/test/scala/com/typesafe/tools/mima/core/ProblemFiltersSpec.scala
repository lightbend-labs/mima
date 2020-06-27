package com.typesafe.tools.mima.core

final class ProblemFiltersSpec extends munit.FunSuite {
  check(ProblemFilters.exclude[Problem]("impl.Http"), problem("impl.Http"),  false)
  check(ProblemFilters.exclude[Problem]("impl.Http"), problem("impl.Http2"), true)
  check(ProblemFilters.exclude[Problem]("impl.*"),    problem("impl.Http"),  false)
  check(ProblemFilters.exclude[Problem]("impl.*"),    problem("impl2.Http"), true)
  check(ProblemFilters.exclude[Problem]("a$Impl*"),   problem("a$Impl$B"),   false)
  check(ProblemFilters.exclude[Problem]("a$Impl*"),   problem("a2$Impl$B"),  true)

  def check[T](
      filter: ProblemFilter,
      problem: Problem,
      realProblem: Boolean,
  )(implicit loc: munit.Location): Unit = {
    test(s"problem filters should filter problems; filter=$filter problem=$problem realProblem=$realProblem") {
      assertEquals(filter(problem), realProblem)
    }
  }

  private def problem(name: String) = FinalClassProblem(new SyntheticClassInfo(NoPackageInfo, name))
}
