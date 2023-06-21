package com.typesafe.tools.mima.lib

import java.io.File

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.util.log.{ ConsoleLogging, Logging }
import com.typesafe.tools.mima.lib.analyze.Analyzer

final class MiMaLib(cp: Seq[File], log: Logging = ConsoleLogging) {
  private val classpath = ClassPath.of(cp.flatMap(ClassPath.fromJarOrDir(_)) :+ ClassPath.base)

  private def createPackage(dirOrJar: File): PackageInfo = {
    ClassPath.fromJarOrDir(dirOrJar).fold(createEmptyPackage(dirOrJar)) { cp =>
      val defs = new Definitions(ClassPath.of(List(cp, classpath)))
      val pkg = new DefinitionsTargetPackageInfo(defs.root)
      for (pkgName <- cp.packages(ClassPath.RootPackage)) {
        pkg.packages(pkgName) = new ConcretePackageInfo(pkg, cp, pkgName, defs)
      }
      log.debug(s"adding packages from $dirOrJar: ${pkg.packages.keys.mkString(", ")}")
      pkg
    }
  }

  private def createEmptyPackage(missingDirOrJar: File): PackageInfo = {
    log.debug(s"not a directory or jar file: $missingDirOrJar.  This is normal for POM-only modules.  Proceeding with empty set of packages.")
    val defs = new Definitions(classpath)
    new DefinitionsTargetPackageInfo(defs.root)
  }

  private def traversePackages(oldpkg: PackageInfo, newpkg: PackageInfo, excludeAnnots: List[AnnotInfo]): List[Problem] = {
    log.verbose(s"traversing $oldpkg")
    Analyzer.analyze(oldpkg, newpkg, log, excludeAnnots) ++ oldpkg.packages.values.toSeq.sortBy(_.name).flatMap { p =>
      val q = newpkg.packages.getOrElse(p.name, NoPackageInfo)
      traversePackages(p, q, excludeAnnots)
    }
  }

  /** Return a list of problems for the two versions of the library. */
  def collectProblems(oldJarOrDir: File, newJarOrDir: File, excludeAnnots: List[String]): List[Problem] = {
    val oldPackage = createPackage(oldJarOrDir)
    val newPackage = createPackage(newJarOrDir)
    log.debug(s"[old version in: ${oldPackage.definitions}]")
    log.debug(s"[new version in: ${newPackage.definitions}]")
    log.debug(s"classpath: ${classpath.asClassPathString}")
    traversePackages(oldPackage, newPackage, excludeAnnots.map(AnnotInfo(_)))
  }
}
