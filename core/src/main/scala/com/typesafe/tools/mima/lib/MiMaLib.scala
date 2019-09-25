package com.typesafe.tools.mima.lib

import java.io.File

import com.typesafe.tools.mima.core.Config._
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.util.log.{ ConsoleLogging, Logging }
import com.typesafe.tools.mima.lib.analyze.Analyzer

import scala.collection.mutable.ListBuffer
import scala.reflect.io.{ AbstractFile, Path }
import scala.tools.nsc.util.ClassPath

final class MiMaLib(classpath: ClassPath, log: Logging = ConsoleLogging) {
  private def createDefinitions(dirOrJar: File): Definitions = {
    AbstractFile.getDirectory(Path(dirOrJar)) match {
      case null => fatal(s"not a directory or jar file: $dirOrJar")
      case dir  => new Definitions(Some(dirClassPath(dir)), classpath)
    }
  }

  private val problems = new ListBuffer[Problem]

  private def raise(problem: Problem) = {
    problems += problem
    log.debugLog(s"Problem: ${problem.description("new")}")
  }

  private def comparePackages(oldpkg: PackageInfo, newpkg: PackageInfo): Unit = {
    for (oldclazz <- oldpkg.accessibleClasses) {
      log.info(s"Analyzing $oldclazz")
      newpkg.classes.get(oldclazz.bytecodeName) match {
        case Some(newclazz) => Analyzer(oldclazz, newclazz).foreach(raise)
        case None           =>
          // if it is missing a trait implementation class, then no error should be reported
          // since there should be already errors, i.e., missing methods...
          if (!oldclazz.isImplClass)
            raise(MissingClassProblem(oldclazz))
      }
    }
  }

  private def traversePackages(oldpkg: PackageInfo, newpkg: PackageInfo): Unit = {
    log.info(s"Traversing $oldpkg")
    comparePackages(oldpkg, newpkg)
    for (p <- oldpkg.packages.valuesIterator) {
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
    problems.toList
  }
}
