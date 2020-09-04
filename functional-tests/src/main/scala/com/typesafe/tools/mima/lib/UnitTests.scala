package com.typesafe.tools.mima.lib

import scala.reflect.io.Path
import scala.util.{ Failure, Success, Try }

import munit.GenericTest

object UnitTests {
  def main(args: Array[String]): Unit       = TestCli.argsToTests(args.toList, runTestCase).unsafeRunTest()
  def munitTests(): List[GenericTest[Unit]] = TestCli.argsToTests(Nil, runTestCase).munitTests

  def runTestCase(testCase: TestCase): Try[Unit] = for {
    () <- testNameCheck(testCase)
    () <- CollectProblemsTest.testCollectProblems(testCase)
    () <- AppRunTest.testAppRun(testCase)
  } yield ()

  def testNameCheck(testCase: TestCase): Try[Unit] = {
    val emptyProblemsTxt = blankFile(testCase.baseDir / "problems.txt")
    testCase.baseDir.name.takeRight(4).dropWhile(_ != '-') match {
      case "-ok"  => if (emptyProblemsTxt) Success(()) else Failure(new Exception("OK test with non-empty problems.txt"))
      case "-nok" => if (emptyProblemsTxt) Failure(new Exception("NOK test with empty problems.txt")) else Success(())
      case _      => Failure(new Exception("Missing '-ok' or '-nok' suffix in project name"))
    }
  }

  def blankFile(p: Path): Boolean = p.toFile.lines().forall(_.startsWith("#"))
}
