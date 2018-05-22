package com.typesafe.tools.mima.plugin

import com.typesafe.tools.mima.core.util.log.Logging
import com.typesafe.tools.mima.core._
import sbt._
import org.scalatest.{Matchers, WordSpec}

class ProblemReportingSpec extends WordSpec with Matchers {
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
    SbtMima.isReported("test" % "module" % moduleVersion, filters, Map.empty)(NoOpLogger, "test")(MissingFieldProblem(NoMemberInfo))
  private def isReported(moduleVersion: String, versionedFilters: Map[String, Seq[ProblemFilter]]) =
    SbtMima.isReported("test" % "module" % moduleVersion, Seq.empty, versionedFilters)(NoOpLogger, "test")(MissingFieldProblem(NoMemberInfo))

}

object ProblemReportingSpec {
  final val NoOpLogger = new Logging {
    override def info(str: String): Unit = ()
    override def debugLog(str: String): Unit = ()
    override def error(str: String): Unit = ()
  }

  final val NoMemberInfo = new MemberInfo(NoClass, "", 0, "")

  final val AllMatchingFilter = (_: Problem) => false
}
