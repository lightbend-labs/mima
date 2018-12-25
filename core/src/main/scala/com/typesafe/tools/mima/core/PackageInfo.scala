package com.typesafe.tools.mima.core

import scala.collection.mutable
import scala.annotation.tailrec
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.classpath.AggregateClassPath
import scala.tools.nsc.util.ClassPath

object PackageInfo {
  final private[core] def NoPackageInfo = com.typesafe.tools.mima.core.NoPackageInfo

  val classExtension = ".class"
  val implClassSuffix = "$class"

  def className(fileName: String) = {
    assert(fileName endsWith classExtension)
    fileName.substring(0, fileName.length - classExtension.length)
  }

  def traitName(iclassName: String) = {
    assert(iclassName endsWith implClassSuffix)
    iclassName.substring(0, iclassName.length - implClassSuffix.length)
  }
}

import com.typesafe.tools.mima.core.PackageInfo._

class SyntheticPackageInfo(owner: PackageInfo, val name: String) extends PackageInfo(owner) {
  def definitions: Definitions = owner.definitions
  lazy val packages = mutable.Map.empty[String, PackageInfo]
  lazy val classes = mutable.Map.empty[String, ClassInfo]
}

object NoPackageInfo extends SyntheticPackageInfo(PackageInfo.NoPackageInfo, "<no package>") {
  override val owner = this
  override def isRoot = true
  override def definitions: Definitions = sys.error("Called definitions on NoPackageInfo")
}

/** A concrete package. cp should be a directory classpath.
 */
class ConcretePackageInfo(owner: PackageInfo, cp: ClassPath, val pkg: String, val defs: Definitions) extends PackageInfo(owner) {
  def definitions = defs
  def name = pkg.split('.').last
  private def classFiles: IndexedSeq[AbstractFile] = classFilesFrom(cp, pkg)

  lazy val packages: mutable.Map[String, PackageInfo] =
    mutable.Map() ++= packagesFrom(cp, this)

  lazy val classes: mutable.Map[String, ClassInfo] =
    mutable.Map() ++= (classFiles map (f => className(f.name) -> new ConcreteClassInfo(this, f)))
}

final private[core] class DefinitionsPackageInfo(defs: Definitions)
  extends ConcretePackageInfo(
    NoPackageInfo,
    AggregateClassPath.createAggregate(defs.lib.toList :+ defs.classPath: _*),
    ClassPath.RootPackage,
    defs,
  )
{
  override def isRoot = true
}

final private[core] class DefinitionsTargetPackageInfo(root: ConcretePackageInfo)
  extends SyntheticPackageInfo(root, "<root>")
{
  override def isRoot = true

  /** Needed to fetch classes located in the root (empty package) */
  override lazy val classes = root.classes
}

/** Package information, including available classes and packages, and what is
 *  accessible.
 */
abstract class PackageInfo(val owner: PackageInfo) {

  def name: String

  def definitions: Definitions

  def isRoot = owner == null

  private lazy val root: PackageInfo = if (isRoot) this else owner.root

  def fullName: String = if (isRoot) "<root>"
                         else if (owner.isRoot) name
                         else owner.fullName + "." + name

  def packages: mutable.Map[String, PackageInfo]
  def classes: mutable.Map[String, ClassInfo]

  lazy val accessibleClasses: Set[ClassInfo] = {
    /* Fixed point iteration for finding all accessible classes. */
    @tailrec
    def accessibleClassesUnder(prefix: Set[ClassInfo], found: Set[ClassInfo]): Set[ClassInfo] = {
      val vclasses = (classes.valuesIterator.filter(isAccessible(_, prefix))).toSet
      if (vclasses.isEmpty) found
      else accessibleClassesUnder(vclasses, vclasses union found)
    }

    def isAccessible(clazz: ClassInfo, prefix: Set[ClassInfo]) = {
      def isReachable = {
        if (clazz.isSynthetic) false
        else if (prefix.isEmpty) clazz.isTopLevel && !clazz.decodedName.contains("$$")
        else prefix.exists(_.innerClasses contains clazz.bytecodeName)
      }
      clazz.isPublic && !clazz.isLocalClass && isReachable
    }

    accessibleClassesUnder(Set.empty, Set.empty)
  }

  /** All implementation classes of traits (classes that end in "$" followed by "class"). */
  lazy val implClasses: mutable.Map[String, ClassInfo] =
    classes filter { case (name, _) => name endsWith implClassSuffix }

  lazy val traits : mutable.Map[String, ClassInfo] = for {
    (name, iclazz) <- implClasses
    tclazz <- classes get traitName(name)
  } yield {
    tclazz.implClass = iclazz
    (traitName(name), tclazz)
  }

  override def toString = "package "+name

  def packageString = "package "+fullName
}
