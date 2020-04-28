package com.typesafe.tools.mima.lib

import java.io.File

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.util.log.{ ConsoleLogging, Logging }
import com.typesafe.tools.mima.lib.analyze.Analyzer

import scala.reflect.io.{ AbstractFile, Path }
import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.mima.ClassPathAccessors
import scala.tools.nsc.util.ClassPath

final class MiMaLib(cp: Seq[File], scalaVersion: String, log: Logging = ConsoleLogging) {
  locally {
    scalaVersion.take(5) match {
      case "2.11." | "2.12." | "2.13." => () // ok
      case _ => throw new IllegalArgumentException(s"MiMa supports Scala 2.10-2.13, not $scalaVersion")
    }
  }

  val classpath =
    AggregateClassPath.createAggregate(cp.flatMap(pathToClassPath(_)) :+ Config.baseClassPath: _*)

  private def createPackage(dirOrJar: File): PackageInfo = {
    val cp = pathToClassPath(dirOrJar).getOrElse(Config.fatal(s"not a directory or jar file: $dirOrJar"))
    val defs = new Definitions(AggregateClassPath.createAggregate(cp, classpath))
    val pkg = new DefinitionsTargetPackageInfo(defs.root)
    for (p <- cp.packagesIn(ClassPath.RootPackage)) {
      pkg.packages(p.name) = new ConcretePackageInfo(pkg, cp, p.name, defs)
    }
    log.debug(s"added packages to <root>: ${pkg.packages.keys.mkString(", ")}")
    pkg
  }

  private def comparePackages(oldpkg: PackageInfo, newpkg: PackageInfo): List[Problem] = {
    for {
      oldclazz <- oldpkg.accessibleClasses.toList
      _ = log.verbose(s"Analyzing $oldclazz")
      problem <- newpkg.classes.get(oldclazz.bytecodeName) match {
        case Some(newclazz) => Analyzer.analyze(oldclazz, newclazz)
        case None           =>
          // if it is missing a trait implementation class, then no error should be reported
          // since there should be already errors, i.e., missing methods...
          if (oldclazz.isImplClass) Nil
          else List(MissingClassProblem(oldclazz))
      }
    } yield {
      log.debug(s"Problem: ${problem.description("new")}")
      problem
    }
  }

  private def traversePackages(oldpkg: PackageInfo, newpkg: PackageInfo): List[Problem] = {
    log.verbose(s"Traversing $oldpkg")
    comparePackages(oldpkg, newpkg) ++ oldpkg.packages.valuesIterator.flatMap { p =>
      val q = newpkg.packages.getOrElse(p.name, NoPackageInfo)
      traversePackages(p, q)
    }
  }

  /** Return a list of problems for the two versions of the library. */
  def collectProblems(oldJarOrDir: File, newJarOrDir: File): List[Problem] = {
    val oldPackage = createPackage(oldJarOrDir)
    val newPackage = createPackage(newJarOrDir)
    log.debug(s"[old version in: ${oldPackage.definitions}]")
    log.debug(s"[new version in: ${newPackage.definitions}]")
    log.debug(s"classpath: ${classpath.asClassPathString}")
    traversePackages(oldPackage, newPackage)
  }

  private def pathToClassPath(p: Path): Option[ClassPath] =
    Option(AbstractFile.getDirectory(p)).map(DeprecatedPathApis.newClassPath(_, Config.settings))
}
