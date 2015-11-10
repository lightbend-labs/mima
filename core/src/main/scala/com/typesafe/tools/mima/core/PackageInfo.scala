package com.typesafe.tools.mima.core

import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.util.ClassPath
import collection.mutable

object PackageInfo {
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

import PackageInfo._

class SyntheticPackageInfo(owner: PackageInfo, val name: String) extends PackageInfo(owner) {
  def definitions: Definitions = sys.error("Called definitions on synthetic package")
  lazy val packages = mutable.Map.empty[String, PackageInfo]
  lazy val classes = mutable.Map.empty[String, ClassInfo]
}

object NoPackageInfo extends SyntheticPackageInfo(null, "<no package>")

/** A concrete package. cp should be a directory classpath.
 */
class ConcretePackageInfo(owner: PackageInfo, cp: ClassPath[AbstractFile], val defs: Definitions) extends PackageInfo(owner) {
  def definitions = defs
  def name = cp.name
  private def classFiles: IndexedSeq[AbstractFile] = cp.classes flatMap (_.binary)

  lazy val packages: mutable.Map[String, PackageInfo] =
    mutable.Map() ++= (cp.packages map (cp => cp.name -> new ConcretePackageInfo(this, cp, defs)))

  lazy val classes: mutable.Map[String, ClassInfo] =
    mutable.Map() ++= (classFiles map (f => className(f.name) -> new ConcreteClassInfo(this, f)))
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
    /** Fixed point iteration for finding all accessible classes. */
    def accessibleClassesUnder(prefix: Set[ClassInfo]): Set[ClassInfo] = {
      val vclasses = (classes.valuesIterator filter (isAccessible(_, prefix))).toSet
      if (vclasses.isEmpty) vclasses
      else vclasses union accessibleClassesUnder(vclasses)
    }

    def isAccessible(clazz: ClassInfo, prefix: Set[ClassInfo]) = {
      def isReachable = {
        if (clazz.isSynthetic) false
        else {
          val idx = clazz.decodedName.lastIndexOf("$")
          if (idx < 0) prefix.isEmpty // class name contains no $
          else prefix exists (_.decodedName == clazz.decodedName.substring(0, idx)) // prefix before dollar is an accessible class detected previously
        }
      }
      clazz.isPublic && isReachable
    }

    accessibleClassesUnder(Set.empty)
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

