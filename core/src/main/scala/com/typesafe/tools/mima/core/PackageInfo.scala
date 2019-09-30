package com.typesafe.tools.mima.core

import scala.annotation.tailrec
import scala.collection.mutable
import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.mima.ClassPathAccessors
import scala.tools.nsc.util.ClassPath
import com.typesafe.tools.mima.core.util.log.ConsoleLogging

sealed class SyntheticPackageInfo(val owner: PackageInfo, val name: String) extends PackageInfo {
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

sealed class ConcretePackageInfo(val owner: PackageInfo, cp: ClassPath, pkg: String, defs: Definitions)
    extends PackageInfo
{
  def name        = pkg.split('.').last
  def definitions = defs

  private def classFiles = cp.classesIn(pkg).flatMap(_.binary)

  lazy val packages = {
    cp.packagesIn(pkg).iterator.map { p =>
      p.name.stripPrefix(s"$pkg.") -> new ConcretePackageInfo(this, cp, p.name, defs)
    }.to[({type M[_] = mutable.Map[String, PackageInfo]})#M]
  }

  lazy val classes = {
    classFiles.iterator.map { f =>
      val c = new ConcreteClassInfo(this, f)
      c.bytecodeName -> c
    }.toMap
  }
}

final private[core] class DefinitionsPackageInfo(defs: Definitions)
    extends ConcretePackageInfo(
      NoPackageInfo,
      AggregateClassPath.createAggregate(defs.lib.toList :+ defs.classPath: _*),
      ClassPath.RootPackage,
      defs,
    )

private[core] object DefinitionsTargetPackageInfo {
  def create(defs: Definitions): PackageInfo = {
    val pkg = new DefinitionsTargetPackageInfo(defs.root)
    val cp = defs.lib.getOrElse(AggregateClassPath(Nil))
    for (p <- cp.packagesIn(ClassPath.RootPackage)) {
      pkg.packages(p.name) = new ConcretePackageInfo(pkg, cp, p.name, defs)
    }
    ConsoleLogging.debugLog(pkg.packages.keys.mkString("added packages to <root>: ", ", ", ""))
    pkg
  }
}

final private[core] class DefinitionsTargetPackageInfo(root: PackageInfo)
  extends SyntheticPackageInfo(root, "<root>")
{
  // Needed to fetch classes located in the root (empty package).
  override lazy val classes = root.classes
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

  final def isRoot: Boolean = this match {
    case NoPackageInfo                   => true
    case _: DefinitionsPackageInfo       => true
    case _: DefinitionsTargetPackageInfo => true
    case _: ConcretePackageInfo          => false
    case _: SyntheticPackageInfo         => false
  }

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
