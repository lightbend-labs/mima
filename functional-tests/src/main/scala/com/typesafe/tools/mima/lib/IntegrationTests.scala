package com.typesafe.tools.mima.lib

import java.io.File

import scala.collection.JavaConverters._
import scala.reflect.io.Directory
import scala.util.Try

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
      v1 <- getArtifact(groupId, artifactId, conf.getString("v1"))
      v2 <- getArtifact(groupId, artifactId, conf.getString("v2"))
      problemFilter <- getProblemFilter(baseDir.jfile)
      problems = CollectProblemsTest.collectProblems(v1, v2).filter(problemFilter)
      expected = CollectProblemsTest.readOracleFile((baseDir / "problems.txt").jfile)
      () <- CollectProblemsTest.diffProblems(problems, expected)
    } yield ()
  }

  def getArtifact(groupId: String, artifactId: String, version: String): Try[File] = {
    val dep = Dependency(Module(Organization(groupId), ModuleName(artifactId)), version)
    val allFiles = Coursier.fetch(dep.withTransitive(false))
    Try(allFiles.headOption.getOrElse(sys.error(s"Could not resolve artifact: $dep")))
  }

  def getProblemFilter(baseDir: File): Try[Problem => Boolean] = Try {
    val configFile = new File(baseDir, "test.conf")
    val configFallback = ConfigFactory.parseString("filter { problems = [] }")
    val config = ConfigFactory.parseFile(configFile).withFallback(configFallback).resolve()
    val filters = for (conf <- config.getConfigList("filter.problems").asScala)
      yield ProblemFilters.exclude(conf.getString("problemName"), conf.getString("matchName"))
    (p: Problem) => filters.forall(filter => filter(p))
  }
}
