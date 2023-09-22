package com.typesafe.tools.mima.core

import scala.annotation.{nowarn, tailrec}
import scala.collection.mutable

sealed class SyntheticPackageInfo(val owner: PackageInfo, val name: String) extends PackageInfo {
  def definitions   = owner.definitions
  lazy val packages = mutable.Map.empty[String, PackageInfo]
  lazy val classes  = Map.empty[String, ClassInfo]
}

@nowarn("msg=under -Xsource:3, inferred")  // return types are a bit different between 2 and 3 but it's fine afaics
object NoPackageInfo extends PackageInfo {
  val name        = "<no package>"
  val owner        = this
  def definitions = sys.error("Called definitions on NoPackageInfo")
  val packages    = mutable.Map.empty[String, PackageInfo]
  val classes     = Map.empty[String, ClassInfo]
}

sealed class ConcretePackageInfo(val owner: PackageInfo, cp: ClassPath, pkg: String, defs: Definitions)
    extends PackageInfo
{
  def name        = pkg.split('.').last
  def definitions = defs

  lazy val packages: mutable.Map[String, PackageInfo] =
    // this way of building the map cross-compiles on 2.12 and 2.13 without
    // needing to bring in scala-collection-compat
    mutable.Map() ++
      cp.packages(pkg).map { p =>
        p.stripPrefix(s"$pkg.") -> new ConcretePackageInfo(this, cp, p, defs)
      }

  lazy val classes =
    cp.classes(pkg).map { f =>
      val c = new ConcreteClassInfo(this, f)
      c.bytecodeName -> c
    }.toMap
}

final private[core] class DefinitionsPackageInfo(defs: Definitions)
    extends ConcretePackageInfo(NoPackageInfo, defs.classPath, ClassPath.RootPackage, defs)

final private[mima] class DefinitionsTargetPackageInfo(root: PackageInfo)
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
    @tailrec def loop(found: Set[ClassInfo], prefix: Set[ClassInfo]): Set[ClassInfo] = {
      val accessibleClasses = classes.valuesIterator.filter(isAccessible(_, prefix)).toSet
      if (accessibleClasses.isEmpty) found
      else loop(accessibleClasses ++ found, accessibleClasses)
    }

    def isAccessible(clazz: ClassInfo, prefix: Set[ClassInfo]) =
      clazz.isPublic && !clazz.isLocalClass && !clazz.isSynthetic && isReachable(clazz, prefix)

    def isReachable(clazz: ClassInfo, prefix: Set[ClassInfo]) = {
      if (prefix.isEmpty) clazz.isTopLevel && !clazz.decodedName.contains("$$")
      else prefix.exists(_.innerClasses.contains(clazz.bytecodeName))
    }

    loop(Set.empty, Set.empty)
  }

  // Used to make sure trait classes have their impl class field set
  final lazy val setImplClasses: Unit = {
    for {
      (name, clazz) <- classes.iterator
      if clazz.isImplClass
      traitClass <- classes.get(name.stripSuffix("$class"))
    } {
      traitClass._implClass = clazz
    }
  }

  // TODO: Foo contains pickle, so if parse Foo$ before should be able to set this then
  final lazy val setModules: Unit = {
    for {
      (name, clazz) <- classes.iterator
      if clazz.isModuleClass
      module <- classes.get(name.init)
    } {
      clazz._module       = module
      module._moduleClass = clazz
    }
  }

  final override def toString = s"package $fullName"
}
