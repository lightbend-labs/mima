package com.typesafe.tools.mima.lib

import java.io.File
import java.nio.file.Files

import com.typesafe.tools.mima.core.Problem

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

object CollectProblemsTest {
  def testCollectProblems(testCase: TestCase, direction: Direction): Try[Unit] = for {
    () <- testCase.compileBoth
    problems = collectProblems(cp = Nil, testCase.outV1.jfile, testCase.outV2.jfile, direction)
    expected = readOracleFile(testCase.versionedFile(direction.oracleFile).jfile)
    () <- diffProblems(problems, expected, direction)
  } yield ()

  def collectProblems(cp: Seq[File], v1: File, v2: File, direction: Direction): List[Problem] = {
    val (lhs, rhs) = direction.ordered(v1, v2)
    new MiMaLib(cp).collectProblems(lhs, rhs)
  }

  def readOracleFile(oracleFile: File): List[String] = {
    Files.lines(oracleFile.toPath).iterator.asScala.filter(!_.startsWith("#")).toList
  }

  def diffProblems(problems: List[Problem], expected: List[String], direction: Direction): Try[Unit] = {
    val affectedVersion = direction match {
      case Backwards => "new"
      case Forwards  => "other"
    }

    val reported = problems.map(_.description(affectedVersion))

    val msg = new StringBuilder("\n")
    def pp(start: String, lines: List[String]) =
      if (lines.nonEmpty) {
        msg.append(s"$start (${lines.size}):")
        lines.sorted.distinct.map("\n  - " + _).foreach(msg.append(_))
        msg.append("\n")
      }

    pp("The following problem(s) were expected but not reported", expected.diff(reported))
    pp("The following problem(s) were reported but not expected", reported.diff(expected))

    msg.mkString match {
      case "\n" => Success(())
      case msg  =>
        Console.err.println(msg)
        Failure(new Exception("CollectProblemsTest failure", null, false, false) {})
    }
  }
}
