package com.typesafe.tools.mima.lib

import scala.reflect.io.Path
import scala.util.{ Failure, Success, Try }

object UnitTests {
  def main(args: Array[String]): Unit = TestCli.argsToTests(args.toList, runTestCase).unsafeRunTest()
  def munitTests(): List[munit.Test]  = TestCli.argsToTests(Nil, runTestCase).munitTests

  def runTestCase(testCase: TestCase): Try[Unit] = for {
    () <- runTestCaseIf(testCase, Backwards)
    () <- runTestCaseIf(testCase, Forwards)
  } yield ()

  def runTestCaseIf(testCase: TestCase, direction: Direction): Try[Unit] = {
    if ((testCase.baseDir / direction.oracleFile).exists)
      runTestCase1(testCase, direction)
    else
      Success(())
  }

  def runTestCase1(testCase: TestCase, direction: Direction): Try[Unit] = for {
    () <- testNameCheck(testCase, direction.oracleFile)
    () <- CollectProblemsTest.testCollectProblems(testCase, direction)
    () <- AppRunTest.testAppRun(testCase, direction)
  } yield ()

  def testNameCheck(testCase: TestCase, oracleFile: Path): Try[Unit] = {
    val emptyOracleFile = testCase.blankFile(testCase.baseDir / oracleFile)
    testCase.name.takeRight(4).dropWhile(_ != '-') match {
      case "-ok"  => if (emptyOracleFile) Success(()) else Failure(new Exception(s"OK test with non-empty ${oracleFile.name}"))
      case "-nok" => if (emptyOracleFile) Failure(new Exception(s"NOK test with empty ${oracleFile.name}")) else Success(())
      case _      => Failure(new Exception(s"Missing '-ok' or '-nok' suffix in project name: ${testCase.name}"))
    }
  }
}
