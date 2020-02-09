package com.typesafe.tools.mima.core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class ProblemReportingSpec extends AnyWordSpec with Matchers {
  "isReported" should handle {
    "no filters"       in assert(isReported(Nil))
    "filters"          in assert(!isReported(List(excludeAll)))

    "version filter"   in assert(!isReportedV("0.0.1", "0.0.1"))
    "wildcard"         in assert(!isReportedV("0.0.1", "0.0.x"))
    "no patch segment" in assert(!isReportedV("0.1", "0.1"))
    "only major"       in assert(!isReportedV("1", "1"))
    "less segments"    in assert(!isReportedV("0.1.0", "0.1"))
    "more segments"    in assert(!isReportedV("0.1", "0.1.0"))
  }

  private def isReported(filters: List[ProblemFilter]) = helper("0.0.1", filters, Map.empty)

  private def isReportedV(version: String, filterVersion: String) =
    helper(version, Nil, Map(filterVersion -> Seq(excludeAll)))

  private def helper(v: String, fs: Seq[ProblemFilter], vfs: Map[String, Seq[ProblemFilter]]) =
    ProblemReporting.isReported(v, fs, vfs)(FinalClassProblem(NoClass))

  private def excludeAll = ProblemFilters.exclude[Problem]("*")
  private def handle     = afterWord("handle")
}
