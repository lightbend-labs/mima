package com.typesafe.tools.mima

import scala.tools.nsc.util.ClassPath
import scala.tools.nsc.classpath.{AggregateClassPath, ClassPathFactory}
import scala.tools.nsc.io.AbstractFile
import scala.tools.util.PathResolver
import scala.tools.nsc.mima.ClassPathAccessors

package object core {
  type ProblemFilter = (Problem) => Boolean
  type CompilerClassPath = ClassPath

  def resolveClassPath(): CompilerClassPath =
    AggregateClassPath.createAggregate(new PathResolver(Config.settings).containers: _*)

  def definitionsPackageInfo(defs: Definitions): ConcretePackageInfo = {
      new ConcretePackageInfo(
        null,
        AggregateClassPath.createAggregate(defs.cp, defs.classPath),
        ClassPath.RootPackage,
        defs)
    }

  def asClassPathString(cp: CompilerClassPath): String = cp.asClassPathString

  def classFilesFrom(cp: CompilerClassPath, pkg: String): IndexedSeq[AbstractFile] =
    cp.classesIn(pkg).flatMap(_.binary).toIndexedSeq

  def packagesFrom(cp: CompilerClassPath, owner: ConcretePackageInfo): Seq[(String, PackageInfo)] =
    (cp.packagesIn(owner.pkg) map (p => p.name.stripPrefix(owner.pkg + ".") -> new ConcretePackageInfo(owner, cp, p.name, owner.defs)))

  def definitionsTargetPackages(pkg: PackageInfo, cp: CompilerClassPath, defs: Definitions): Seq[(String, PackageInfo)] =
    cp.packagesIn(ClassPath.RootPackage).map(p => p.name -> new ConcretePackageInfo(pkg, cp, p.name, defs))

  def baseClassPath(cpString: String): CompilerClassPath =
    AggregateClassPath.createAggregate(new ClassPathFactory(Config.settings).classesInPath(cpString): _*)

  def reporterClassPath(classpath: String): CompilerClassPath = AggregateClassPath.createAggregate(
    new ClassPathFactory(Config.settings).classesInPath(classpath): _*)

  def dirClassPath(dir: AbstractFile): CompilerClassPath =
    ClassPathFactory.newClassPath(dir, Config.settings)
}
