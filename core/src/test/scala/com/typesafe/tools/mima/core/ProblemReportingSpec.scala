package com.typesafe.tools.mima.core

final class ProblemReportingSpec extends munit.FunSuite {
  test("isReported should handle no filters")(assert(isReported(Nil)))
  test("isReported should handle filters")(assert(!isReported(List(excludeAll))))

  test("isReported should handle version filter")(assert(!isReportedV("0.0.1", "0.0.1")))
  test("isReported should handle wildcard")(assert(!isReportedV("0.0.1", "0.0.x")))
  test("isReported should handle no patch segment")(assert(!isReportedV("0.1", "0.1")))
  test("isReported should handle only major")(assert(!isReportedV("1", "1")))
  test("isReported should handle less segments")(assert(!isReportedV("0.1.0", "0.1")))
  test("isReported should handle more segments")(assert(!isReportedV("0.1", "0.1.0")))

  test("isReported should handle later version")(assert(!isReportedV("1.2.3", filterVersion = "1.9.9")))
  test("isReported should handle earlier version")(assert(isReportedV("1.2.3", filterVersion = "1.1.6")))

  private def isReported(filters: List[ProblemFilter]) = helper("0.0.1", filters, Map.empty)

  private def isReportedV(version: String, filterVersion: String) =
    helper(version, Nil, Map(filterVersion -> Seq(excludeAll)))

  private def helper(v: String, fs: Seq[ProblemFilter], vfs: Map[String, Seq[ProblemFilter]]) =
    ProblemReporting.isReported(v, fs, vfs)(FinalClassProblem(NoClass))

  private def excludeAll = ProblemFilters.exclude[Problem]("*")
}
