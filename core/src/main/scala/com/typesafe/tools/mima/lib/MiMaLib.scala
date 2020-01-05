package com.typesafe.tools.mima.lib

import java.io.File

import com.typesafe.tools.mima.core.Config._
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.util.log.{ ConsoleLogging, Logging }
import com.typesafe.tools.mima.lib.analyze.Analyzer

import scala.tools.nsc.util.ClassPath

final class MiMaLib(classpath: ClassPath, log: Logging = ConsoleLogging) {
  private def createDefinitions(dirOrJar: File): Definitions = {
    pathToClassPath(dirOrJar) match {
      case None => fatal(s"not a directory or jar file: $dirOrJar")
      case cp   => new Definitions(cp, classpath)
    }
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
    val oldDefinitions = createDefinitions(oldJarOrDir)
    val newDefinitions = createDefinitions(newJarOrDir)
    log.debugLog(s"[old version in: $oldDefinitions]")
    log.debugLog(s"[new version in: $newDefinitions]")
    log.debugLog(s"classpath: ${classpath.asClassPathString}")
    traversePackages(oldDefinitions.targetPackage, newDefinitions.targetPackage)
  }
}
