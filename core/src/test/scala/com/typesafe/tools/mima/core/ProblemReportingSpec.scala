package com.typesafe.tools.mima.core

import org.scalatest.{ Assertion, Matchers, WordSpec }

final class ProblemReportingSpec extends WordSpec with Matchers {
  import ProblemReportingSpec._

  // Given a generic Problem (FinalClassProblem)
  "isReported" should {
    "return true on no filters"                         in (check("0.0.1", Nil)      shouldBe true)
    "return false when filtered out by general filters" in (check("0.0.1", KeepNone) shouldBe false)

    "match when filter is used"                               in checkVersioned("0.0.1", "0.0.1")
    "match when filter uses a wildcard"                       in checkVersioned("0.0.1", "0.0.x")
    "match when filter does not have patch segment"           in checkVersioned("0.1", "0.1")
    "match when filter is only epoch"                         in checkVersioned("1", "1")
    "match when filter has less segments than module version" in checkVersioned("0.1.0", "0.1")
    "match when filter has more segments than module version" in checkVersioned("0.1", "0.1.0")
  }

  private def check(version: String, filters: Seq[ProblemFilter]): Boolean = {
    impl(version, filters, Map.empty)
  }

  private def checkVersioned(version: String, filterVersion: String): Assertion = {
    impl(version, Nil, Map(filterVersion -> KeepNone)) shouldBe false
  }

  private def impl(v: String, pfs: Seq[ProblemFilter], pfMap: Map[String, Seq[ProblemFilter]]) = {
    ProblemReporting.isReported(v, pfs, pfMap)(FinalClassProblem(NoClass))
  }
}

object ProblemReportingSpec {
  final val KeepNone: Seq[ProblemFilter] = Seq(Function.const(false))
}
