package com.typesafe.tools.mima.lib

import java.io.File

import com.typesafe.config.ConfigFactory
import com.typesafe.tools.mima.core.Problem

import scala.io.Source

object CollectProblemsTest {

  // Called via reflection from TestsPlugin
  def runTest(cp: Array[File], testName: String, oldJarOrDir: File, newJarOrDir: File, baseDir: File, oracleFile: File): Unit = {
    val mima = new MiMaLib(cp)

    val configFile = new File(baseDir, "test.conf")
    val configFallback = ConfigFactory.parseString("filter { problems = [] }")
    val config = ConfigFactory.parseFile(configFile).withFallback(configFallback).resolve()
    val filters = ProblemFiltersConfig.parseProblemFilters(config)
    val problemFilter = (p: Problem) => filters.forall(filter => filter(p))

    val problems = mima.collectProblems(oldJarOrDir, newJarOrDir).filter(problemFilter)

    // load oracle
    val source = Source.fromFile(oracleFile)
    val expectedProblems = try source.getLines.filter(!_.startsWith("#")).toList finally source.close()

    // diff between the oracle and the collected problems
    val problemDescriptions = problems.iterator.map(_.description("new")).toSet
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

  final case class TestFailed(msg: String) extends Exception(msg)
}
