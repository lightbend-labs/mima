package ssol.tools.mima

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
  lazy val classpath = Config.classpath
  lazy val root = new ConcretePackageInfo(null, classpath)
  Config.info("classpath = "+classpath)
}

import PackageInfo._

class SyntheticPackageInfo(owner: PackageInfo, val name: String) extends PackageInfo(owner) {
  lazy val packages: mutable.Map[String, PackageInfo] = mutable.Map()
  lazy val classes: mutable.Map[String, ClassInfo] = mutable.Map()
}

class ConcretePackageInfo(owner: PackageInfo, cp: ClassPath[AbstractFile]) extends PackageInfo(owner) {
  def name = cp.name
  private def classFiles: List[AbstractFile] = cp.classes flatMap (_.binary)

  lazy val packages: mutable.Map[String, PackageInfo] = 
    mutable.Map() ++= (cp.packages map (cp => cp.name -> new ConcretePackageInfo(this, cp)))

  lazy val classes: mutable.Map[String, ClassInfo] = 
    mutable.Map() ++= (classFiles map (f => className(f.name) -> new ConcreteClassInfo(this, f)))
}

abstract class PackageInfo(val owner: PackageInfo) {

  def name: String

  def isRoot = owner == null

  def fullName: String = if (isRoot) "<root>" 
                         else if (owner.isRoot) name
                         else owner.fullName + "." + name

  def packages: mutable.Map[String, PackageInfo]
  def classes: mutable.Map[String, ClassInfo]

  private def isAccessible(clazz: ClassInfo, prefix: Set[ClassInfo]) = {
    val idx = clazz.name.lastIndexOf("$")
    val isReachable = 
      if (idx < 0) prefix.isEmpty // class name contains no $
      else (prefix exists (_.name == clazz.name.substring(0, idx))) // prefix before dollar is an accessible class detected previously
    isReachable && clazz.isPublic
  }

  private def accessibleClassesUnder(prefix: Set[ClassInfo]): Set[ClassInfo] = {
    val vclasses = (classes.valuesIterator filter (isAccessible(_, prefix))).toSet
    if (vclasses.isEmpty) vclasses
    else vclasses union accessibleClassesUnder(vclasses)
  }

  lazy val accessibleClasses: Set[ClassInfo] = accessibleClassesUnder(Set())
  
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

