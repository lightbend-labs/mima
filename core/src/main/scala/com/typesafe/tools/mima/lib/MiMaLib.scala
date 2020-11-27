package com.typesafe.tools.mima.lib

import java.io.File

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.util.log.{ ConsoleLogging, Logging }
import com.typesafe.tools.mima.lib.analyze.Analyzer

final class MiMaLib(cp: Seq[File], log: Logging = ConsoleLogging) {
  private val classpath = ClassPath.of(cp.flatMap(ClassPath.fromJarOrDir(_)) :+ ClassPath.base)

  private def createPackage(dirOrJar: File): PackageInfo = {
    val cp = ClassPath.fromJarOrDir(dirOrJar).getOrElse(sys.error(s"not a directory or jar file: $dirOrJar"))
    val defs = new Definitions(ClassPath.of(List(cp, classpath)))
    val pkg = new DefinitionsTargetPackageInfo(defs.root)
    for (pkgName <- cp.packages(ClassPath.RootPackage)) {
      pkg.packages(pkgName) = new ConcretePackageInfo(pkg, cp, pkgName, defs)
    }
    log.debug(s"adding packages from $dirOrJar: ${pkg.packages.keys.mkString(", ")}")
    pkg
  }

  private def traversePackages(oldpkg: PackageInfo, newpkg: PackageInfo): List[Problem] = {
    log.verbose(s"traversing $oldpkg")
    Analyzer.analyze(oldpkg, newpkg, log) ++ oldpkg.packages.values.toSeq.sortBy(_.name).flatMap { p =>
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
}
