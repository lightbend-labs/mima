package com.typesafe.tools.mima.lib

import java.io.File

import scala.util.{Failure, Success, Try}

import com.typesafe.tools.mima.core.ProblemFilter
import coursier._

import CollectProblemsTest._, IntegrationTests._

object IntegrationTests {
  def testIntegration(groupId: String, artifactId: String, v1: String, v2: String)(
      expected: List[String] = Nil,
      problemFilters: List[ProblemFilter] = Nil,
      excludeAnnots: List[String] = Nil,
      moduleAttrs: Map[String, String] = Map.empty
  ) = {
    val module = Module(Organization(groupId), ModuleName(artifactId), moduleAttrs)
    for {
      (v1, _) <- fetchArtifact(Dependency(module, v1))
      (v2, cp) <- fetchArtifact(Dependency(module, v2))
      () <- collectAndDiff(cp, v1, v2)(expected, problemFilters, excludeAnnots)
    } yield ()
  }

  def fetchArtifact(dep: Dependency): Try[(File, Seq[File])] =
    Coursier.fetch(dep) match {
      case Seq(jar, cp @ _*) => Success((jar, cp))
      case _                 => Failure(sys.error(s"Could not resolve artifact: $dep"))
    }
}

object CompareJars {
  def main(args: Array[String]): Unit = args.toList match {
    case Seq(file) =>
      runTry(collectAndDiff(Nil, new File(file), new File(file))())
    case Seq(groupId, artifactId, v1, v2, attrStrs @ _*) =>
      val attrs = attrStrs.map { s =>
        val Array(k, v) = s.split('='); k -> v
      }.toMap
      runTry(testIntegration(groupId, artifactId, v1, v2)(moduleAttrs = attrs))
  }

  def runTry(tri: Try[Unit]) = tri match {
    case Success(())  =>
    case Failure(exc) => System.err.println(s"$exc"); throw new Exception("fail")
  }
}
