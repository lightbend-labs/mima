package com.typesafe.tools.mima

import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.util.{ClassPath, DirectoryClassPath, JavaClassPath}

package object core {
  type ProblemFilter = (Problem) => Boolean
  type CompilerClassPath = scala.tools.nsc.util.ClassPath[AbstractFile]

  def resolveClassPath(): CompilerClassPath =
    new PathResolver(Config.settings).mimaResult

  def definitionsPackageInfo(defs: Definitions): ConcretePackageInfo =
    new ConcretePackageInfo(null,
      new JavaClassPath(
        if (defs.lib.isDefined) Vector(defs.lib.get, defs.classPath)
        else Vector(defs.classPath), DefaultJavaContext),
      "",
      defs)

  def asClassPathString(cp: CompilerClassPath): String = cp.asClasspathString

  def classFilesFrom(cp: CompilerClassPath, pkg: String): IndexedSeq[AbstractFile] =
    cp.classes flatMap (_.binary)

  def packagesFrom(cp: CompilerClassPath, owner: ConcretePackageInfo): Seq[(String, PackageInfo)] =
    (cp.packages map (cp => cp.name -> new ConcretePackageInfo(owner, cp, owner.pkg + "." + cp.name, owner.defs)))

  def definitionsTargetPackages(pkg: PackageInfo, cp: CompilerClassPath, defs: Definitions): Seq[(String, PackageInfo)] =
    cp.packages.map(p => p.name -> new ConcretePackageInfo(pkg, p, p.name, defs))

  def baseClassPath(cpString: String): CompilerClassPath =
    new JavaClassPath(ClassPath.DefaultJavaContext.classesInPath(cpString).toIndexedSeq, ClassPath.DefaultJavaContext)

  def reporterClassPath(classpath: String): CompilerClassPath =
    new JavaClassPath(DefaultJavaContext.classesInPath(classpath).toIndexedSeq, DefaultJavaContext)

  def dirClassPath(dir: AbstractFile): CompilerClassPath =
    new DirectoryClassPath(dir, DefaultJavaContext)
}
