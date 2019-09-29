package com.typesafe.tools.mima.core

import scala.annotation.tailrec
import scala.collection.mutable
import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.mima.ClassPathAccessors
import scala.tools.nsc.util.ClassPath
import com.typesafe.tools.mima.core.util.log.ConsoleLogging

final private[core] class EmptyPackage(val owner: PackageInfo, val name: String) extends PackageInfo {
  def definitions   = owner.definitions
  lazy val packages = mutable.Map.empty[String, PackageInfo]
  lazy val classes  = Map.empty[String, ClassInfo]
}

object NoPackageInfo extends PackageInfo {
  val name        = "<no package>"
  val owner       = this
  def definitions = sys.error("Called definitions on NoPackageInfo")
  val packages    = mutable.Map.empty[String, PackageInfo]
  val classes     = Map.empty[String, ClassInfo]
}

final class ConcretePackageInfo(val owner: PackageInfo, cp: ClassPath, pkg: String, defs: Definitions)
    extends PackageInfo
{
  def name        = pkg.split('.').last
  def definitions = defs

  private def classFiles = cp.classesIn(pkg).flatMap(_.binary)

  lazy val packages = {
    cp.packagesIn(pkg).iterator.map { p =>
      p.name.stripPrefix(s"$pkg.") -> new ConcretePackageInfo(owner, cp, p.name, defs)
    }.to[({type M[_] = mutable.Map[String, PackageInfo]})#M]
  }

  lazy val classes = {
    classFiles.iterator.map { f =>
      val c = new ConcreteClassInfo(this, f)
      c.bytecodeName -> c
    }.toMap
  }
}

private[core] object TargetRootPackageInfo {
  def create(defs: Definitions): PackageInfo = {
    val pkg = new TargetRootPackageInfo(defs)
    val cp = defs.lib.getOrElse(AggregateClassPath(Nil))
    for (p <- cp.packagesIn(ClassPath.RootPackage)) {
      pkg.packages(p.name) = new ConcretePackageInfo(pkg, cp, p.name, defs)
    }
    ConsoleLogging.debugLog(pkg.packages.keys.mkString("added packages to <root>: ", ", ", ""))
    pkg
  }
}

final private[core] class TargetRootPackageInfo(val definitions: Definitions) extends PackageInfo {
  def owner         = definitions.root
  def name          = "<root>"
  lazy val packages = mutable.Map.empty[String, PackageInfo]
  lazy val classes  = definitions.root.classes // Fetch classes located in the root/empty package
}

/** Package information, including available classes and packages, and what is accessible. */
sealed abstract class PackageInfo {
  def name: String
  def owner: PackageInfo
  def definitions: Definitions
  def packages: mutable.Map[String, PackageInfo]
  def classes: Map[String, ClassInfo]

  final def fullName: String = {
    if (isRoot) "<root>"
    else if (owner.isRoot) name
    else s"${owner.fullName}.$name"
  }

  final def isRoot: Boolean = owner == NoPackageInfo || owner == definitions.root

  final lazy val accessibleClasses: Set[ClassInfo] = {
    // Fixed point iteration for finding all accessible classes.
    @tailrec
    def accessibleClassesUnder(prefix: Set[ClassInfo], found: Set[ClassInfo]): Set[ClassInfo] = {
      val vclasses = (classes.valuesIterator.filter(isAccessible(_, prefix))).toSet
      if (vclasses.isEmpty) found
      else accessibleClassesUnder(vclasses, vclasses.union(found))
    }

    def isAccessible(clazz: ClassInfo, prefix: Set[ClassInfo]) = {
      def isReachable = {
        if (prefix.isEmpty) clazz.isTopLevel && !clazz.decodedName.contains("$$")
        else prefix.exists(_.innerClasses.contains(clazz.bytecodeName))
      }
      clazz.isPublic && !clazz.isLocalClass && !clazz.isSynthetic && isReachable
    }

    accessibleClassesUnder(Set.empty, Set.empty)
  }

  final lazy val setImplClasses: Unit = {
    for {
      (name, clazz) <- classes.iterator
      if clazz.isImplClass
      traitClass <- classes.get(name.stripSuffix("$class"))
    } {
      traitClass.implClass = clazz
    }
  }

  final override def toString = s"package $fullName"
}
