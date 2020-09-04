package com.typesafe.tools.mima.lib

import scala.util.{ Failure, Success, Try }

/** Test running the App, using library v2. */
object AppRunTest {
  def main(args: Array[String]): Unit = TestCli.argsToTests(args.toList, testAppRun).unsafeRunTest()

  def testAppRun(testCase: TestCase): Try[Unit] = for {
    () <- testCase.compileThem
    pending = testCase.versionedFile("testAppRun.pending").exists
    emptyPT = testCase.blankFile(testCase.versionedFile("problems.txt"))
    () <- testCase.runMain(testCase.outV1) // expect at least v1 to pass, even in the "pending" case
    () <- testCase.runMain(testCase.outV2) match {
      case Failure(t)  if !pending &&  emptyPT => Failure(t)
      case Success(()) if !pending && !emptyPT => Failure(new Exception("expected running App to fail"))
      case _                                   => Success(())
    }
  } yield ()
}
