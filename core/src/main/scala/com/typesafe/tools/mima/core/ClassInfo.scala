package com.typesafe.tools.mima.core

import scala.reflect.io.AbstractFile
import scala.reflect.NameTransformer

import com.typesafe.tools.mima.core.util.log.ConsoleLogging

object ClassInfo {
  def formatClassName(str: String) = NameTransformer.decode(str).replace('$', '#')

  /** We assume there can be only one java.lang.Object class,
   *  and that comes from the configuration class path.
   */
  lazy val ObjectClass = {
    val baseClassPath = DeprecatedPathApis.newPathResolver(Config.settings).result
    val baseDefinitions = new Definitions(None, baseClassPath)
    baseDefinitions.ObjectClass
  }
}

/** A placeholder class info for a class that is not found on the classpath or in a given package. */
sealed class SyntheticClassInfo(owner: PackageInfo, val bytecodeName: String) extends ClassInfo(owner) {
  loaded = true
  def file: AbstractFile = throw new UnsupportedOperationException

  override lazy val superClasses        = Set(ClassInfo.ObjectClass)
  final override lazy val allTraits     = Set.empty[ClassInfo]
  final override lazy val allInterfaces = Set.empty[ClassInfo]

  override def canEqual(other: Any) = other.isInstanceOf[SyntheticClassInfo]
}

object NoClass extends SyntheticClassInfo(NoPackageInfo, "<noclass>") {
  override lazy val superClasses = Set.empty[ClassInfo]

  override def canEqual(other: Any) = other.isInstanceOf[NoClass.type]
}

/** A class for which we have the classfile. */
final class ConcreteClassInfo(owner: PackageInfo, val file: AbstractFile) extends ClassInfo(owner) {
  def bytecodeName                  = file.name.stripSuffix(".class")
  override def canEqual(other: Any) = other.isInstanceOf[ConcreteClassInfo]
}

sealed abstract class ClassInfo(val owner: PackageInfo) extends InfoLike with Equals {
  import ClassInfo._

  def file: AbstractFile

  final protected var loaded: Boolean = false

  private def afterLoading[A](x: => A): A = {
    if (!loaded)
      try {
        ConsoleLogging.info(s"parsing $file")
        ClassfileParser.parseInPlace(this, file)
      } finally {
        loaded = true
      }
    x
  }

  final var _innerClasses: Seq[String]   = Nil
  final var _isLocalClass: Boolean       = false
  final var _isTopLevel: Boolean         = true
  final var _superClass: ClassInfo       = NoClass
  final var _interfaces: List[ClassInfo] = Nil
  final var _fields: Fields              = NoMembers
  final var _methods: Methods            = NoMembers
  final var _flags: Int                  = 0
  final var _implClass: ClassInfo        = NoClass

  final def innerClasses: Seq[String]   = afterLoading(_innerClasses)
  final def isLocalClass: Boolean       = afterLoading(_isLocalClass)
  final def isTopLevel: Boolean         = afterLoading(_isTopLevel)
  final def superClass: ClassInfo       = afterLoading(_superClass)
  final def interfaces: List[ClassInfo] = afterLoading(_interfaces)
  final def fields: Fields              = afterLoading(_fields)
  final def methods: Methods            = afterLoading(_methods)
  final def flags: Int                  = afterLoading(_flags)

  lazy val fullName: String = {
    assert(bytecodeName != null)
    if (owner.isRoot) bytecodeName
    else owner.fullName + "." + bytecodeName
  }

  final override def equals(other: Any): Boolean = other match {
    case that: ClassInfo => that.canEqual(this) && this.fullName == that.fullName
    case _               => false
  }

  final override def hashCode = this.fullName.hashCode

  override def canEqual(other: Any) = other.isInstanceOf[ClassInfo]

  def formattedFullName = formatClassName(if (isModule) fullName.init else fullName)

  def declarationPrefix = {
    if (isModule) "object"
    else if (isTrait) "trait"
    else if (isInterface) "interface" // java interfaces and traits with no implementation methods
    else "class"
  }

  def classString: String    = s"$accessModifier $declarationPrefix $formattedFullName".trim
  def accessModifier: String = if (isProtected) "protected" else if (isPrivate) "private" else ""

  def superClass_=(x: ClassInfo) = _superClass = x
  def interfaces_=(x: List[ClassInfo]) = _interfaces = x
  def fields_=(x: Fields) = _fields = x
  def methods_=(x: Methods) = _methods = x
  def flags_=(x: Int) = _flags = x

  lazy val superClasses: Set[ClassInfo] =
    if (this == ClassInfo.ObjectClass) Set.empty
    else superClass.superClasses + superClass

  private def thisAndSuperClasses = Iterator.single(this) ++ superClasses.iterator

  final def lookupClassFields(field: FieldInfo): Iterator[FieldInfo] =
    thisAndSuperClasses.flatMap(_.fields.get(field.bytecodeName))

  final def lookupClassMethods(method: MethodInfo): Iterator[MethodInfo] = {
    method.bytecodeName match {
      case MemberInfo.ConstructorName => methods.get(MemberInfo.ConstructorName) // constructors are not inherited
      case name                       => thisAndSuperClasses.flatMap(_.methods.get(name))
    }
  }

  private def lookupInterfaceMethods(method: MethodInfo): Iterator[MethodInfo] =
    allInterfaces.iterator.flatMap(_.methods.get(method.bytecodeName))

  final def lookupMethods(method: MethodInfo): Iterator[MethodInfo] =
    lookupClassMethods(method) ++ lookupInterfaceMethods(method)

  final def lookupConcreteTraitMethods(method: MethodInfo): Iterator[MethodInfo] =
    allTraits.iterator.flatMap(_.concreteMethods).filter(_.bytecodeName == method.bytecodeName)

  /** The concrete methods of this trait. */
  final lazy val concreteMethods: List[MethodInfo] = {
    if (isTrait) methods.value.filter(m => hasStaticImpl(m) || m.isConcrete)
    else if (isClass || isInterface) methods.value.filter(_.isConcrete)
    else Nil
  }

  /** The subset of concrete methods of this trait that are abstract at the JVM level.
    * This corresponds to the pre-Scala-2.12 trait encoding where all `concreteMethods`
    * are `emulatedConcreteMethods`. In 2.12 most concrete trait methods are translated
    * to concrete interface methods.
    */
  final lazy val emulatedConcreteMethods: List[MethodInfo] = concreteMethods.filter(_.isDeferred)

  /** The deferred methods of this trait. */
  final lazy val deferredMethods: List[MethodInfo] = {
    val concreteMethods = this.concreteMethods.toSet
    methods.value.filterNot(concreteMethods)
  }

  /** All deferred methods of this type as seen in the bytecode. */
  final def deferredMethodsInBytecode: List[MethodInfo] = if (isTrait) methods.value else deferredMethods

  /** The inherited traits in the linearization of this class or trait,
   *  except any traits inherited by its superclass.
   *  Traits appear in linearization order of this class or trait.
   */
  final lazy val directTraits: List[ClassInfo] = {
    val superClassTraits = superClass.allTraits.toSet

    // All traits in the transitive, reflexive inheritance closure of the given trait.
    def traitClosure(t: ClassInfo): List[ClassInfo] = {
      if (superClassTraits.contains(t)) Nil
      // traits with only abstract methods are presented as interfaces,
      // but nonetheless they should still be collected
      else if (t.isInterface) parentsClosure(t) :+ t
      else parentsClosure(t)
    }

    def parentsClosure(c: ClassInfo) = c.interfaces.flatMap(traitClosure).distinct

    parentsClosure(this)
  }

  /** All traits inherited directly or indirectly by this class. */
  lazy val allTraits: Set[ClassInfo] = {
    if (this == ClassInfo.ObjectClass) Set.empty
    else superClass.allTraits ++ directTraits
  }

  /** All interfaces inherited directly or indirectly by this class. */
  lazy val allInterfaces: Set[ClassInfo] = {
    if (this == ClassInfo.ObjectClass) Set.empty
    else superClass.allInterfaces ++ interfaces ++ interfaces.flatMap(_.allInterfaces)
  }

  /** Does this implementation class have a static implementation of given method `m`? */
  final def hasStaticImpl(m: MethodInfo): Boolean = {
    implClass match {
      case _: SyntheticClassInfo   => false
      case impl: ConcreteClassInfo =>
        assert(impl.isImplClass, impl)
        impl.methods.get(m.bytecodeName).exists(im => hasImplSig(im.descriptor, m.descriptor))
    }
  }

  // Does `isig` correspond to `tsig` if seen as the signature of the static
  // implementation method of a trait method with signature `tsig`?
  private def hasImplSig(isig: String, tsig: String): Boolean = {
    assert(isig(0) == '(' && isig(1) == 'L' && tsig(0) == '(')
    val ilen = isig.length
    val tlen = tsig.length
    var i = 2
    while (isig(i) != ';')
      i += 1
    i += 1
    var j = 1
    while (i < ilen && j < tlen && isig(i) == tsig(j)) {
      i += 1
      j += 1
    }
    i == ilen && j == tlen
  }

  /** Is this class an implementation class? */
  lazy val isImplClass: Boolean = bytecodeName.endsWith("$class")

  def implClass_=(ic: ClassInfo) = _implClass = ic

  /** The implementation class of this trait, or NoClass if it is not a trait.
   */
  def implClass: ClassInfo = {
    owner.setImplClasses // make sure we have implClass set
    _implClass
  }

  /** is this a class, an object or a trait's implementation class*/
  def isClass: Boolean = !isTrait && !isInterface

  /** Is this class a trait with some concrete methods or fields? */
  def isTrait: Boolean = implClass ne NoClass

  /** Is this class a trait without concrete methods or a java interface? */
  def isInterface: Boolean = ClassfileParser.isInterface(flags)

  def isModule: Boolean = bytecodeName.endsWith("$")

  override def toString = s"class $bytecodeName"

  def description: String = s"$declarationPrefix $formattedFullName"
}
