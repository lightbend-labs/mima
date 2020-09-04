package com.typesafe.tools.mima.lib

import java.io.File
import java.nio.file.Files

import com.typesafe.tools.mima.core.Problem

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

object CollectProblemsTest {
  def testCollectProblems(testCase: TestCase, direction: Direction): Try[Unit] = for {
    () <- testCase.compileBoth
    problems = collectProblems(direction.lhs(testCase).jfile, direction.rhs(testCase).jfile)
    expected = readOracleFile(testCase.versionedFile(direction.oracleFile).jfile)
    () <- direction match {
      case Backwards => diffProblems(problems, expected)
      case Forwards  => diffProblems(problems, expected, "other")
    }
  } yield ()

  val mimaLib: MiMaLib = new MiMaLib(cp = Nil)

  def collectProblems(v1: File, v2: File): List[Problem] = mimaLib.collectProblems(v1, v2)

  def readOracleFile(oracleFile: File): List[String] = {
    Files.lines(oracleFile.toPath).iterator.asScala.filter(!_.startsWith("#")).toList
  }

  def diffProblems(problems: List[Problem], expected: List[String], v: String = "new"): Try[Unit] = {
    // diff between the oracle and the collected problems
    val unexpected = problems.filter(p => !expected.contains(p.description(v)))
    val unreported = expected.diff(problems.map(_.description(v)))

    val msg = new StringBuilder("\n")
    def pp(start: String, lines: List[String]) = {
      if (lines.isEmpty) ()
      else lines.sorted.distinct.addString(msg, s"$start (${lines.size}):\n  - ", "\n  - ", "\n")
    }
    pp("The following problem(s) were expected but not reported", unreported)
    pp("The following problem(s) were reported but not expected", unexpected.map(_.description(v)))
    pp("Filter with:", unexpected.flatMap(_.howToFilter))
    pp("Or filter with:", unexpected.flatMap(p => p.matchName.map { matchName =>
      s"{ matchName=$dq$matchName$dq , problemName=${p.getClass.getSimpleName} }"
    }))

    msg.mkString match {
      case "\n" => Success(())
      case msg  => Failure(new Exception(msg))
    }
  }

  private final val dq = '"' // scala/bug#6476 -.-
}
