package com.typesafe.tools.mima.core

import com.typesafe.tools.mima.core.util.log.Logging

import org.scalatest.Matchers
import org.scalatest.wordspec.AnyWordSpec

final class ProblemReportingSpec extends AnyWordSpec with Matchers {
  import ProblemReportingSpec._

  "problem" should {
    "be reported when there are no filters" in {
      isReported("0.0.1", Seq.empty) shouldBe true
    }

    "not be reported when filtered out by general filters" in {
      isReported("0.0.1", Seq(AllMatchingFilter)) shouldBe false
    }

    "not be reported when filtered out by versioned filters" in {
      isReported("0.0.1", Map("0.0.1" -> Seq(AllMatchingFilter))) shouldBe false
    }

    "not be reported when filtered out by versioned wildcard filters" in {
      isReported("0.0.1", Map("0.0.x" -> Seq(AllMatchingFilter))) shouldBe false
    }

    "not be reported when filter version does not have patch segment" in {
      isReported("0.1", Map("0.1" -> Seq(AllMatchingFilter))) shouldBe false
    }

    "not be reported when filter version is only epoch" in {
      isReported("1", Map("1" -> Seq(AllMatchingFilter))) shouldBe false
    }

    "not be reported when filter version has less segments than module version" in {
      isReported("0.1.0", Map("0.1" -> Seq(AllMatchingFilter))) shouldBe false
    }

    "not be reported when filter version has more segments than module version" in {
      isReported("0.1", Map("0.1.0" -> Seq(AllMatchingFilter))) shouldBe false
    }
  }

  private def isReported(moduleVersion: String, filters: Seq[ProblemFilter]) =
    ProblemReporting.isReported(moduleVersion, filters, Map.empty)(NoOpLogger, "test", "current")(FinalClassProblem(NoClass))
  private def isReported(moduleVersion: String, versionedFilters: Map[String, Seq[ProblemFilter]]) =
    ProblemReporting.isReported(moduleVersion, Seq.empty, versionedFilters)(NoOpLogger, "test", "current")(FinalClassProblem(NoClass))

}

object ProblemReportingSpec {
  final val NoOpLogger = new Logging {
    override def info(str: String): Unit = ()
    override def debugLog(str: String): Unit = ()
    override def warn(str: String): Unit = ()
    override def error(str: String): Unit = ()
  }

  final val AllMatchingFilter = (_: Problem) => false
}
