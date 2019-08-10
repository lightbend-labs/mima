package com.typesafe.tools.mima.lib

import java.io.File

import com.typesafe.config.ConfigFactory
import com.typesafe.tools.mima.core.{ Config, Problem, asClassPathString, baseClassPath }

import scala.io.Source
import scala.tools.nsc.util._

final case class TestFailed(msg: String) extends Exception(msg)

class CollectProblemsTest {

  def runTest(testClasspath: Array[String])(testName: String, oldJarPath: String, newJarPath: String, oraclePath: String, filterPath: String): Unit = {
    // load test setup
    Config.setup("scala com.typesafe.tools.mima.MiMaLibUI <old-dir> <new-dir>", Array(oldJarPath, newJarPath))
    val cp = testClasspath ++ ClassPath.split(asClassPathString(Config.baseClassPath))
    val cpString = ClassPath.join(cp.toIndexedSeq: _*)
    Config.baseClassPath = baseClassPath(cpString)

    val mima = new MiMaLib(Config.baseClassPath)

    // SUT
    val allProblems = mima.collectProblems(oldJarPath, newJarPath)

    val problems = if (filterPath ne null) {
      val fallback = ConfigFactory.parseString("filter { problems = [] }")
      val config = ConfigFactory.parseFile(new File(filterPath)).withFallback(fallback).resolve()
      val filters = ProblemFiltersConfig.parseProblemFilters(config)
      allProblems.filter(p => filters.forall(_.apply(p)))
    } else allProblems
    val problemDescriptions = problems.iterator.map(_.description("new")).toSet

    // load oracle
    val source = Source.fromFile(oraclePath)
    val expectedProblems = try source.getLines.filter(!_.startsWith("#")).toList finally source.close()

    // diff between the oracle and the collected problems
    val unexpectedProblems = problems.filterNot(p => expectedProblems.contains(p.description("new")))
    val unreportedProblems = expectedProblems.filterNot(problemDescriptions.contains)

    val msg = buildErrorMessage(unexpectedProblems, unreportedProblems)

    if (!msg.isEmpty)
      throw TestFailed(s"'$testName' failed.\n$msg")
  }

  private def buildErrorMessage(unexpectedProblems: List[Problem], unreportedProblems: List[String]): String = {
    val msg = new StringBuilder

    if (unexpectedProblems.nonEmpty) {
      val qt = """""""
      unexpectedProblems.iterator
          .map(_.description("new"))
          .addString(msg, s"The following ${unexpectedProblems.size} problems were reported but not expected:\n  - ", "\n  - ", "\n")
      unexpectedProblems.iterator
          .flatMap(_.howToFilter.map(s => s + ",").toList)
          .toIndexedSeq
          .sorted
          .distinct
          .addString(msg, "Filter with:\n  ", "\n  ", "\n")
      unexpectedProblems.iterator
          .flatMap { p => p.matchName.map(matchName => s"{ matchName=$qt$matchName$qt , problemName=${p.getClass.getSimpleName} }") }
          .toIndexedSeq
          .sorted
          .distinct
          .addString(msg, "Or filter with:\n  ", "\n  ", "\n")
    }

    if (unreportedProblems.nonEmpty)
      //noinspection ScalaUnusedExpression // intellij-scala is wrong, addString _does_ side-effect
      unreportedProblems.addString(msg, s"The following ${unreportedProblems.size} problems were expected but not reported:\n  - ", "\n  - ", "\n")

    msg.result()
  }
}
