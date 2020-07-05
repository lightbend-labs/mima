package com.typesafe.tools.mima.lib

import java.io.File
import java.nio.file.Files

import com.typesafe.config.ConfigFactory
import com.typesafe.tools.mima.core.{ Problem, ProblemFilters }

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

object CollectProblemsTest {
  def main(args: Array[String]): Unit = TestCase.argsToTests(args.toList, testCollectProblems).assert()

  def testCollectProblems(testCase: TestCase) = {
    import testCase._
    for {
      () <- compileThem
      oracleFile = versionedFile(baseDir / "problems.txt")
      () <- runTest(outV1.jfile, outV2.jfile, baseDir.jfile, oracleFile.jfile)
    } yield ()
  }

  def runTest(oldJarOrDir: File, newJarOrDir: File, baseDir: File, oracleFile: File): Try[Unit] = {
    val mima = new MiMaLib(Nil)

    val configFile = new File(baseDir, "test.conf")
    val configFallback = ConfigFactory.parseString("filter { problems = [] }")
    val config = ConfigFactory.parseFile(configFile).withFallback(configFallback).resolve()
    val filters = for (conf <- config.getConfigList("filter.problems").asScala)
      yield ProblemFilters.exclude(conf.getString("problemName"), conf.getString("matchName"))
    val problemFilter = (p: Problem) => filters.forall(filter => filter(p))

    val problems = mima.collectProblems(oldJarOrDir, newJarOrDir).filter(problemFilter)

    // load oracle
    val expectedProblems = Files.lines(oracleFile.toPath).iterator.asScala.filter(!_.startsWith("#")).toList

    // diff between the oracle and the collected problems
    val problemDescriptions = problems.iterator.map(_.description("new")).toSet
    val unexpectedProblems = problems.filterNot(p => expectedProblems.contains(p.description("new")))
    val unreportedProblems = expectedProblems.filterNot(problemDescriptions.contains)

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

    msg.result() match {
      case ""  => Success(())
      case msg => Failure(new Exception(msg))
    }
  }
}
