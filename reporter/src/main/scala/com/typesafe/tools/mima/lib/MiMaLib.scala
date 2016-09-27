package com.typesafe.tools.mima.lib

import com.typesafe.tools.mima.core.Config._
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.util.IndentedOutput._
import com.typesafe.tools.mima.core.util.log.{ConsoleLogging, Logging}
import com.typesafe.tools.mima.lib.analyze.Analyzer

import scala.collection.mutable.ListBuffer
import scala.tools.nsc.classpath.ClassPathFactory
import scala.tools.nsc.io.{AbstractFile, File}
import scala.tools.nsc.util.ClassPath

class MiMaLib(classpath: ClassPath, val log: Logging = ConsoleLogging) {
  /*
  options:
  -classpath foo
  -ignore bar
  -d outputdir
  -i, -iinteractive
  -f, -fix
  */

  private def classPath(name: String) = {
    val f = new File(new java.io.File(name))
    val dir = AbstractFile.getDirectory(f)
    if (dir == null) None
    else Some(ClassPathFactory.newClassPath(dir, Config.settings))
  }

  private def root(name: String): Definitions = classPath(name) match {
    case cp @ Some(_) => new Definitions(cp, classpath)
    case None         => fatal("not a directory or jar file: " + name)
  }

  private val problems = new ListBuffer[Problem]

  private def raise(problem: Problem) = {
    problems += problem
    log.debugLog("Problem: " + problem.description)
  }


  private def comparePackages(oldpkg: PackageInfo, newpkg: PackageInfo) {
    for (oldclazz <- oldpkg.accessibleClasses) {
      log.info("Analyzing class "+oldclazz.bytecodeName)
      newpkg.classes get oldclazz.bytecodeName match {
        case None if oldclazz.isImplClass =>
          // if it is missing a trait implementation class, then no error should be reported
          // since there should be already errors, i.e., missing methods...
          ()

        case None => raise(MissingClassProblem(oldclazz))

        case Some(newclazz) =>  Analyzer(oldclazz, newclazz).foreach(raise)
      }
    }
  }

  private def traversePackages(oldpkg: PackageInfo, newpkg: PackageInfo) {
    log.info("Traversing package " + oldpkg.fullName)
    comparePackages(oldpkg, newpkg)
    indented {
      for (p <- oldpkg.packages.valuesIterator) {
        newpkg.packages get p.name match {
          case None =>
            traversePackages(p, NoPackageInfo)
          case Some(q) =>
            traversePackages(p, q)
        }
      }
    }
  }

  /** Return a list of problems for the two versions of the library. */
  def collectProblems(oldDir: String, newDir: String): List[Problem] = {
    val oldRoot = root(oldDir)
    val newRoot = root(newDir)
    log.debugLog("[old version in: " + oldRoot + "]")
    log.debugLog("[new version in: " + newRoot + "]")
    log.debugLog("classpath: " + Config.baseClassPath.asClassPathString)
    traversePackages(oldRoot.targetPackage, newRoot.targetPackage)
    problems.toList
  }
}
