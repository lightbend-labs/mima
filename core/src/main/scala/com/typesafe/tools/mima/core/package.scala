package com.typesafe.tools.mima

import scala.reflect.io.{ AbstractFile, Path }
import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.mima.ClassPathAccessors
import scala.tools.nsc.util.ClassPath

package object core {
  type ProblemFilter = Problem => Boolean

  import DeprecatedPathApis._

  def definitionsPackageInfo(defs: Definitions): ConcretePackageInfo =
    new DefinitionsPackageInfo(defs)

  def classFilesFrom(cp: ClassPath, pkg: String): IndexedSeq[AbstractFile] =
    cp.classesIn(pkg).flatMap(_.binary).toIndexedSeq

  def packagesFrom(cp: ClassPath, owner: ConcretePackageInfo): Seq[(String, PackageInfo)] =
    cp.packagesIn(owner.pkg).map { p =>
      p.name.stripPrefix(s"${owner.pkg}.") -> new ConcretePackageInfo(owner, cp, p.name, owner.defs)
    }

  def definitionsTargetPackages(pkg: PackageInfo, cp: ClassPath, defs: Definitions): Seq[(String, PackageInfo)] =
    cp.packagesIn(ClassPath.RootPackage).map(p => p.name -> new ConcretePackageInfo(pkg, cp, p.name, defs))

  def baseClassPath(cpString: String): ClassPath =
    AggregateClassPath.createAggregate(newClassPathFactory(Config.settings).classesInPath(cpString): _*)

  def reporterClassPath(classpath: String): ClassPath =
    AggregateClassPath.createAggregate(newClassPathFactory(Config.settings).classesInPath(classpath): _*)

  private[mima] def pathToClassPath(p: Path): Option[ClassPath] =
    Option(AbstractFile.getDirectory(p)).map(newClassPath(_, Config.settings))

  private[core] type Fields  = Members[FieldInfo]
  private[core] type Methods = Members[MethodInfo]
}
