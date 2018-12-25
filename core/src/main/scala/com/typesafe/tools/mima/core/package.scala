package com.typesafe.tools.mima

import scala.tools.nsc.util.ClassPath
import scala.tools.nsc.classpath.{AggregateClassPath, ClassPathFactory}
import scala.tools.nsc.io.AbstractFile
import scala.tools.util.PathResolver
import scala.tools.nsc.mima.ClassPathAccessors

package object core {
  type ProblemFilter = (Problem) => Boolean

  def resolveClassPath(): ClassPath =
    AggregateClassPath.createAggregate(new PathResolver(Config.settings).containers: _*)

  def definitionsPackageInfo(defs: Definitions): ConcretePackageInfo =
    new DefinitionsPackageInfo(defs)

  def asClassPathString(cp: ClassPath): String = cp.asClassPathString

  def classFilesFrom(cp: ClassPath, pkg: String): IndexedSeq[AbstractFile] =
    cp.classesIn(pkg).flatMap(_.binary).toIndexedSeq

  def packagesFrom(cp: ClassPath, owner: ConcretePackageInfo): Seq[(String, PackageInfo)] =
    (cp.packagesIn(owner.pkg) map (p => p.name.stripPrefix(owner.pkg + ".") -> new ConcretePackageInfo(owner, cp, p.name, owner.defs)))

  def definitionsTargetPackages(pkg: PackageInfo, cp: ClassPath, defs: Definitions): Seq[(String, PackageInfo)] =
    cp.packagesIn(ClassPath.RootPackage).map(p => p.name -> new ConcretePackageInfo(pkg, cp, p.name, defs))

  def baseClassPath(cpString: String): ClassPath =
    AggregateClassPath.createAggregate(new ClassPathFactory(Config.settings).classesInPath(cpString): _*)

  def reporterClassPath(classpath: String): ClassPath =
    AggregateClassPath.createAggregate(new ClassPathFactory(Config.settings).classesInPath(classpath): _*)

  def dirClassPath(dir: AbstractFile): ClassPath =
    ClassPathFactory.newClassPath(dir, Config.settings)
}
