package com.typesafe.tools.mima.lib

import java.io.File
import java.nio.file.Files

import com.typesafe.tools.mima.core.ProblemFilter

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object CollectProblemsTest {
  def testCollectProblems(testCase: TestCase, direction: Direction): Try[Unit] = for {
    () <- testCase.compileBoth
    (v1, v2) = direction.ordered(testCase.outV1, testCase.outV2)
    expected = readOracleFile(testCase.versionedFile(direction.oracleFile).jfile)
    () <- collectAndDiff(cp = Nil, v1.jfile, v2.jfile)(
      expected,
      excludeAnnots = excludeAnnots,
      direction     = direction,
    )
  } yield ()

  def collectAndDiff(cp: Seq[File], v1: File, v2: File)(
      expected: List[String]              = Nil,
      problemFilters: List[ProblemFilter] = Nil,
      excludeAnnots: List[String]         = Nil,
      direction: Direction                = Backwards,
  ): Try[Unit] = {
    val problems = new MiMaLib(cp).collectProblems(v1, v2, excludeAnnots).filter(problemFilters.foldAll)
    val affectedVersion = direction match {
      case Backwards => "new"
      case Forwards  => "other"
    }

    val reported = problems.map(_.description(affectedVersion))

    val msg = new StringBuilder("\n")
    import scala.io.AnsiColor._
    final case class DiffType(sign: Char, colour: String)
    val add = DiffType('+', GREEN)
    val del = DiffType('-', RED)

    def pp(start: String, dt: DiffType, lines: List[String]) =
      if (lines.nonEmpty) {
        msg.append(s"$start (${lines.size}):")
        lines.sorted.distinct.map("\n" + dt.colour + dt.sign + _ + RESET).foreach(msg.append)
        msg.append("\n")
      }

    pp("The following problem(s) were expected but not reported", del, expected.diff(reported))
    pp("The following problem(s) were reported but not expected", add, reported.diff(expected))

    msg.mkString match {
      case "\n" => Success(())
      case msg  =>
        Console.err.println(msg)
        Failure(new Exception("CollectProblemsTest failure", null, false, false) {})
    }
  }

  def readOracleFile(oracleFile: File): List[String] = {
    Files.lines(oracleFile.toPath).iterator.asScala.filter(!_.startsWith("#")).toList
  }

  private val excludeAnnots = List("mima.annotation.exclude")

  implicit class PredicatesOps[A](private val ps: Iterable[A => Boolean]) extends AnyVal {
    def foldAll: A => Boolean = (x: A) => ps.forall(p => p(x))
    def foldAny: A => Boolean = (x: A) => ps.exists(p => p(x))
  }
}
