package com.typesafe.tools.mima.lib

import com.typesafe.tools.mima.core.Config
import com.typesafe.tools.mima.core.{asClassPathString, baseClassPath}
import java.io.{BufferedInputStream, File, FileInputStream}

import com.typesafe.config.ConfigFactory
import com.typesafe.tools.mima.core.Config

import scala.io.Source
import scala.tools.nsc.util._

case class TestFailed(msg: String) extends Exception(msg)

class CollectProblemsTest {

  def runTest(testClasspath: Array[String])(testName: String, oldJarPath: String, newJarPath: String, oraclePath: String, filterPath: String): Unit = {
    // load test setup
    Config.setup("scala com.typesafe.tools.mima.MiMaLibUI <old-dir> <new-dir>", Array(oldJarPath, newJarPath))
    val cp = testClasspath ++ ClassPath.split(asClassPathString(Config.baseClassPath))
    val cpString = ClassPath.join(cp: _*)
    Config.baseClassPath = baseClassPath(cpString)

    val mima = new MiMaLib(Config.baseClassPath)

    // SUT
    val allProblems = mima.collectProblems(oldJarPath, newJarPath)

    val problems = (if(filterPath ne null) {
      val fallback = ConfigFactory.parseString("filter { problems = [] }")
      val config = ConfigFactory.parseFile(new File(filterPath)).withFallback(fallback).resolve()
      val filters = ProblemFiltersConfig.parseProblemFilters(config)
      allProblems.filter(p => filters.forall(_.apply(p)))
    } else allProblems).map(_.description("new"))

    // load oracle
    val inputStream = new BufferedInputStream(new FileInputStream(oraclePath))
    var expectedProblems = try {
      Source.fromInputStream(inputStream).getLines.toList
    } finally inputStream.close()

    // diff between the oracle and the collected problems
    val unexpectedProblems = problems.filterNot(expectedProblems.contains)
    val unreportedProblems = expectedProblems.filterNot(problems.contains)

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
