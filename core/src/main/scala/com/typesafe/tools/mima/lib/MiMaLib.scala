package com.typesafe.tools.mima.lib

import java.io.File

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.util.log.{ ConsoleLogging, Logging }
import com.typesafe.tools.mima.lib.analyze.Analyzer

import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.mima.ClassPathAccessors
import scala.tools.nsc.util.ClassPath

final class MiMaLib(classpath: ClassPath, log: Logging = ConsoleLogging) {
  private def createPackage(classpath: ClassPath, dirOrJar: File): PackageInfo = {
    val cp = pathToClassPath(dirOrJar).getOrElse(Config.fatal(s"not a directory or jar file: $dirOrJar"))
    val defs = new Definitions(AggregateClassPath.createAggregate(cp, classpath))
    val pkg = new DefinitionsTargetPackageInfo(defs.root)
    for (p <- cp.packagesIn(ClassPath.RootPackage)) {
      pkg.packages(p.name) = new ConcretePackageInfo(pkg, cp, p.name, defs)
    }
    log.debugLog(s"added packages to <root>: ${pkg.packages.keys.mkString(", ")}")
    pkg
  }

  private def comparePackages(oldpkg: PackageInfo, newpkg: PackageInfo): List[Problem] = {
    for {
      oldclazz <- oldpkg.accessibleClasses.toList
      _ = log.info(s"Analyzing $oldclazz")
      problem <- newpkg.classes.get(oldclazz.bytecodeName) match {
        case Some(newclazz) => Analyzer.analyze(oldclazz, newclazz)
        case None           =>
          // if it is missing a trait implementation class, then no error should be reported
          // since there should be already errors, i.e., missing methods...
          if (oldclazz.isImplClass) Nil
          else List(MissingClassProblem(oldclazz))
      }
    } yield {
      log.debugLog(s"Problem: ${problem.description("new")}")
      problem
    }
  }

  private def traversePackages(oldpkg: PackageInfo, newpkg: PackageInfo): List[Problem] = {
    log.info(s"Traversing $oldpkg")
    comparePackages(oldpkg, newpkg) ++ oldpkg.packages.valuesIterator.flatMap { p =>
      val q = newpkg.packages.getOrElse(p.name, NoPackageInfo)
      traversePackages(p, q)
    }
  }

  /** Return a list of problems for the two versions of the library. */
  def collectProblems(oldJarOrDir: File, newJarOrDir: File): List[Problem] = {
    val oldPackage = createPackage(classpath, oldJarOrDir)
    val newPackage = createPackage(classpath, newJarOrDir)
    log.debugLog(s"[old version in: ${oldPackage.definitions}]")
    log.debugLog(s"[new version in: ${newPackage.definitions}]")
    log.debugLog(s"classpath: ${classpath.asClassPathString}")
    traversePackages(oldPackage, newPackage)
  }
}
