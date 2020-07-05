package com.typesafe.tools.mima.lib

import java.io.File

import com.typesafe.config.ConfigFactory

import scala.reflect.io.Directory
import scala.util.Try

object IntegrationTests {
  def main(args: Array[String]): Unit = fromArgs(args.toList).assert()

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
      () <- CollectProblemsTest.runTest(v1, v2, baseDir.jfile, (baseDir / "problems.txt").jfile)
    } yield ()
  }

  def getArtifact(groupId: String, artifactId: String, version: String): Try[File] = {
    import coursier._
    val dep = Dependency(Module(Organization(groupId), ModuleName(artifactId)), version)
    val allFiles = Coursier.fetch(dep.withTransitive(false))
    Try(allFiles.headOption.getOrElse(sys.error(s"Could not resolve artifact: $dep")))
  }
}
