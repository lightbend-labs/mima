package com.typesafe.tools.mima.lib

import com.typesafe.tools.mima.core.{Config, Settings, PathResolver}
import scala.io.Source
import scala.tools.nsc.util._

case class TestFailed(msg: String) extends Exception(msg)

class CollectProblemsTest {

  def runTest(testClasspath: Array[String])(testName: String, oldJarPath: String, newJarPath: String, oraclePath: String) {
    // load test setup
    Config.setup("scala com.typesafe.tools.mima.MiMaLibUI <old-dir> <new-dir>", Array(oldJarPath, newJarPath))
    val cp = testClasspath ++ ClassPath.split(Config.baseClassPath.asClasspathString)
    val cpString = ClassPath.join(cp: _*)
    Config.baseClassPath = new JavaClassPath(ClassPath.DefaultJavaContext.classesInPath(cpString).toIndexedSeq, ClassPath.DefaultJavaContext)

    val mima = new MiMaLib(Config.baseClassPath)

    // SUT
    val problems = mima.collectProblems(oldJarPath, newJarPath).map(_.description)

    // load oracle
    var expectedProblems = Source.fromFile(oraclePath).getLines.toList 

    // diff between the oracle and the collected problems
    val unexpectedProblems = problems -- expectedProblems
    val unreportedProblems = expectedProblems -- problems

    val mess = buildErrorMessage(unexpectedProblems, unreportedProblems)

    if (!mess.isEmpty)
      throw new TestFailed(testName + "' failed.\n" + mess)
  }

  private def buildErrorMessage(unexpectedProblems: List[String], unreportedProblems: List[String]): String = {
    val mess = new StringBuilder

    if (!unexpectedProblems.isEmpty)
      mess ++= "\tThe following problems were not expected\n" + unexpectedProblems.mkString("\t- ", "\n\t- ", "")

    if (!mess.isEmpty) mess ++= "\n\n"

    if (!unreportedProblems.isEmpty)
      mess ++= "\tThe following expected problems were not reported\n" + unreportedProblems.mkString("\t- ", "\n\t- ", "\n")

    mess.toString
  }
}
