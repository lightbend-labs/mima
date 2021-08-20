package com.typesafe.tools.mima.lib

import java.io.File

import scala.collection.JavaConverters._
import scala.reflect.io.Directory
import scala.util.{ Failure, Success, Try }

import com.typesafe.config.ConfigFactory
import com.typesafe.tools.mima.core.{ Problem, ProblemFilters }
import coursier._
import munit.GenericTest

object IntegrationTests {
  def main(args: Array[String]): Unit       = fromArgs(args.toList).unsafeRunTest()
  def munitTests(): List[GenericTest[Unit]] = fromArgs(Nil).munitTests

  def fromArgs(args: List[String]): Tests = {
    val dirs = Directory("functional-tests/src/it").dirs.filter(args match {
      case Seq() => dir => dir.files.exists(_.name == "test.conf")
      case names => dir => names.contains(dir.name)
    }).toList.sortBy(_.path)
    Tests(dirs.map(dir => Test(dir.name, testIntegration(dir))))
  }

  def testIntegration(baseDir: Directory): Try[Unit] = {
    val conf       = ConfigFactory.parseFile((baseDir / "test.conf").jfile).resolve()
    val groupId    = conf.getString("groupId")
    val artifactId = conf.getString("artifactId")
    for {
      (v1,  _) <- getArtifact(groupId, artifactId, conf.getString("v1"))
      (v2, cp) <- getArtifact(groupId, artifactId, conf.getString("v2"))
      problemFilter <- getProblemFilter(baseDir.jfile)
      () <- testCollectProblems(baseDir, problemFilter, cp, v1, v2, Backwards)
    //() <- testCollectProblems(baseDir, problemFilter, cp, v1, v2, Forwards)
    } yield ()
  }

  def getArtifact(groupId: String, artifactId: String, version: String): Try[(File, Seq[File])] = {
    fetchArtifact(Dependency(Module(Organization(groupId), ModuleName(artifactId)), version))
  }

  def fetchArtifact(dep: Dependency): Try[(File, Seq[File])] = {
    Coursier.fetch(dep) match {
      case Nil               => Failure(sys.error(s"Could not resolve artifact: $dep"))
      case Seq(jar, cp @ _*) => Success((jar, cp))
    }
  }

  def getProblemFilter(baseDir: File): Try[Problem => Boolean] = Try {
    val configFile = new File(baseDir, "test.conf")
    val configFallback = ConfigFactory.parseString("filter { problems = [] }")
    val config = ConfigFactory.parseFile(configFile).withFallback(configFallback).resolve()
    val filters = for (conf <- config.getConfigList("filter.problems").asScala)
      yield ProblemFilters.exclude(conf.getString("problemName"), conf.getString("matchName"))
    (p: Problem) => filters.forall(filter => filter(p))
  }

  def testCollectProblems(baseDir: Directory, problemFilter: Problem => Boolean, cp: Seq[File], v1: File, v2: File, direction: Direction): Try[Unit] = {
    val problems = CollectProblemsTest.collectProblems(cp, v1, v2, direction).filter(problemFilter)
    val expected = CollectProblemsTest.readOracleFile((baseDir / direction.oracleFile).jfile)
    CollectProblemsTest.diffProblems(problems, expected, direction)
  }
}

object CompareJars {
  def main(args: Array[String]): Unit = args.toList match {
    case Seq(file) => runTry(compare(new File(file), new File(file), Nil))
    case Seq(groupId, artifactId, version1, version2, attrStrs @ _*) =>
      val attrs = attrStrs.map { s => val Array(k, v) = s.split('='); k -> v }.toMap
      val module = Module(Organization(groupId), ModuleName(artifactId)).withAttributes(attrs)
      runTry(for {
        (v1, _)  <- IntegrationTests.fetchArtifact(Dependency(module, version1))
        (v2, cp) <- IntegrationTests.fetchArtifact(Dependency(module, version2))
        ()       <- compare(v1, v2, cp)
      } yield ())
  }

  def compare(v1: File, v2: File, cp: Seq[File], direction: Direction = Backwards): Try[Unit] = {
    val problems = CollectProblemsTest.collectProblems(cp, v1, v2, direction)
    CollectProblemsTest.diffProblems(problems, Nil, direction)
  }

  def runTry(tri: Try[Unit]) = tri match {
    case Success(())  =>
    case Failure(exc) => System.err.println(s"$exc"); throw new Exception("fail")
  }
}
