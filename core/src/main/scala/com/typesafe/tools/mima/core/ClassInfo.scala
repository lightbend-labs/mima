package com.typesafe.tools.mima.core

import scala.reflect.io.AbstractFile
import scala.reflect.NameTransformer

import com.typesafe.tools.mima.core.util.log.ConsoleLogging

object ClassInfo {
  def formatClassName(str: String) = NameTransformer.decode(str).replace('$', '#')

  /** We assume there can be only one java.lang.Object class, and that comes from the configuration
   *  class path.
   */
  lazy val ObjectClass = Config.baseDefinitions.ObjectClass
}

/** A placeholder class info for a class that is not found on the classpath or in a given
 *  package.
 */
class SyntheticClassInfo(owner: PackageInfo, override val bytecodeName: String) extends ClassInfo(owner) {
  loaded = true
  def file: AbstractFile = throw new UnsupportedOperationException
  override lazy val superClasses = Set(ClassInfo.ObjectClass)
  override lazy val allTraits = Set.empty[ClassInfo]
  override lazy val allInterfaces: Set[ClassInfo] = Set.empty[ClassInfo]
  override def canEqual(other: Any) = other.isInstanceOf[SyntheticClassInfo]
}

/** As the name implies. */
object NoClass extends SyntheticClassInfo(NoPackageInfo, "<noclass>") {
  override def canEqual(other: Any) = other.isInstanceOf[NoClass.type]
  override lazy val superClasses = Set.empty[ClassInfo]
}

/** A class for which we have the classfile. */
class ConcreteClassInfo(owner: PackageInfo, val file: AbstractFile) extends ClassInfo(owner) {
  override def bytecodeName = PackageInfo.className(file.name)
  override def canEqual(other: Any) = other.isInstanceOf[ConcreteClassInfo]
}

abstract class ClassInfo(val owner: PackageInfo) extends InfoLike with Equals {
  import ClassInfo._

  def file: AbstractFile

  lazy val fullName: String = {
    assert(bytecodeName != null)
    if (owner.isRoot) bytecodeName
    else owner.fullName + "." + bytecodeName
  }

  var _innerClasses: Seq[String] = Seq.empty
  def innerClasses = { ensureLoaded(); _innerClasses }

  var _isLocalClass = false
  def isLocalClass = { ensureLoaded(); _isLocalClass}

  var _isTopLevel = true
  def isTopLevel = { ensureLoaded(); _isTopLevel }

  final override def equals(other: Any): Boolean = other match {
    case that: ClassInfo => (that canEqual this) && this.fullName == that.fullName
    case _               => false
  }

  final override def hashCode = this.fullName.hashCode

  override def canEqual(other: Any) = other.isInstanceOf[ClassInfo]

  def formattedFullName = formatClassName(if (isModule) fullName.init else fullName)

  def declarationPrefix = {
    if (isModule) "object"
    else if (isTrait) "trait"
    else if (loaded && isInterface) "interface" // java interfaces and traits with no implementation methods
    else "class"
  }

  def classString: String    = s"$accessModifier $declarationPrefix $formattedFullName".trim
  def accessModifier: String = if (isProtected) "protected" else if (isPrivate) "private" else ""

  protected var loaded = false

  override protected def ensureLoaded() =
    if (!loaded)
      try {
        ConsoleLogging.info(s"parsing $file")
        ClassfileParser.parseInPlace(this, file)
      } finally {
        loaded = true
      }

  private var _superClass: ClassInfo = NoClass
  private var _interfaces: List[ClassInfo] = Nil
  private var _fields: Fields = NoMembers
  private var _methods: Methods = NoMembers
  private var _flags: Int = 0

  def superClass: ClassInfo = { ensureLoaded(); _superClass }
  def interfaces: List[ClassInfo] = { ensureLoaded(); _interfaces }
  def fields: Fields = { ensureLoaded(); _fields }
  def methods: Methods = { ensureLoaded(); _methods }
  override def flags: Int = _flags

  def superClass_=(x: ClassInfo) = _superClass = x
  def interfaces_=(x: List[ClassInfo]) = _interfaces = x
  def fields_=(x: Fields) = _fields = x
  def methods_=(x: Methods) = _methods = x
  def flags_=(x: Int) = _flags = x

  lazy val superClasses: Set[ClassInfo] =
    if (this == ClassInfo.ObjectClass) Set.empty
    else superClass.superClasses + superClass

  def lookupClassFields(name: String): Iterator[FieldInfo] =
    (Iterator.single(this) ++ superClasses.iterator) flatMap (_.fields.get(name))

  def lookupClassMethods(name: String): Iterator[MethodInfo] =
    if (name == MemberInfo.ConstructorName) methods.get(name) // constructors are not inherited
    else (Iterator.single(this) ++ superClasses.iterator) flatMap (_.methods.get(name))

  private def lookupInterfaceMethods(name: String): Iterator[MethodInfo] =
    allInterfaces.iterator flatMap (_.methods.get(name))

  def lookupMethods(name: String): Iterator[MethodInfo] =
    lookupClassMethods(name) ++ lookupInterfaceMethods(name)

  def lookupConcreteTraitMethods(name: String): Iterator[MethodInfo] =
    allTraits.toList.flatten(_.concreteMethods).filter(_.bytecodeName == bytecodeName).iterator

  /** The concrete methods of this trait */
  lazy val concreteMethods: List[MethodInfo] = {
    if (isTrait) methods.iterator.filter(m => hasStaticImpl(m) || !m.isDeferred).toList
    else if (isClass || isInterface) methods.iterator.filter(!_.isDeferred).toList
    else Nil
  }

  /** The subset of concrete methods of this trait that are abstract at the JVM level.
    * This corresponds to the pre-Scala-2.12 trait encoding where all `concreteMethods`
    * are `emulatedConcreteMethods`. In 2.12 most concrete trait methods are translated
    * to concrete interface methods. */
  lazy val emulatedConcreteMethods: List[MethodInfo] =
    concreteMethods.filter(_.isDeferred)

  /** The deferred methods of this trait */
  lazy val deferredMethods: List[MethodInfo] =
    methods.iterator.toList.filterNot(concreteMethods.toSet)

  /** All deferred methods of this type as seen in the bytecode. */
  def deferredMethodsInBytecode: List[MethodInfo] =
    if (isTrait) methods.iterator.toList
    else deferredMethods

  /** The inherited traits in the linearization of this class or trait,
   *  except any traits inherited by its superclass.
   *  Traits appear in linearization order of this class or trait.
   */
  lazy val directTraits: List[ClassInfo] = {
    /* All traits in the transitive, reflexive inheritance closure of given trait `t' */
    def traitClosure(t: ClassInfo): List[ClassInfo] =
      if (superClass.allTraits contains t) Nil
      // traits with only abstract methods are presented as interfaces, but nonetheless 
      // they should still be collected
      else if (t.isInterface) parentsClosure(t) :+ t
      else parentsClosure(t)

    def parentsClosure(c: ClassInfo) =
      (c.interfaces flatMap traitClosure).distinct

    parentsClosure(this)
  }

  /** All traits inherited directly or indirectly by this class */
  lazy val allTraits: Set[ClassInfo] =
    if (this == ClassInfo.ObjectClass) Set.empty
    else superClass.allTraits ++ directTraits

  /** All interfaces inherited directly or indirectly by this class */
  lazy val allInterfaces: Set[ClassInfo] =
    if (this == ClassInfo.ObjectClass) Set.empty
    else superClass.allInterfaces ++ interfaces ++ (interfaces flatMap (_.allInterfaces))

  /** Does this implementation class have a static implementation of given method `m`? */
  def hasStaticImpl(m: MemberInfo) = staticImpl(m).isDefined

  /** Optionally, the static implementation method corresponding to trait member `m`. */
  private def staticImpl(m: MemberInfo): Option[MemberInfo] = {
    if (isTrait) {
      implClass match {
        case impl: ConcreteClassInfo =>
          assert(impl.isImplClass, impl)
          impl.methods.get(m.bytecodeName) find (im => hasImplSig(im.descriptor, m.descriptor))

        case _ => None
      }
    }
    else None
  }

  /** Does `isig` correspond to `tsig` if seen as the signature of the static
   *  implementation method of a trait method with signature `tsig`?
   */
  private def hasImplSig(isig: String, tsig: String) = {
    assert(isig(0) == '(' && isig(1) == 'L' && tsig(0) == '(')
    val ilen = isig.length
    val tlen = tsig.length
    var i = 2
    while (isig(i) != ';') i += 1
    i += 1
    var j = 1
    while (i < ilen && j < tlen && isig(i) == tsig(j)) {
      i += 1
      j += 1
    }
    i == ilen && j == tlen
  }

  /** Is this class an implementation class? */
  lazy val isImplClass: Boolean = bytecodeName endsWith PackageInfo.implClassSuffix

  /** The implementation class corresponding to this trait */
  private var _implClass: ClassInfo = NoClass

  def implClass_=(ic: ClassInfo) = _implClass = ic

  /** The implementation class of this trait, or NoClass if it is not a trait.
   */
  def implClass: ClassInfo = {
    owner.traits // make sure we have implClass set
    _implClass
  }

  /** is this a class, an object or a trait's implementation class*/
  def isClass: Boolean = !isTrait && !isInterface

  /** Is this class a trait with some concrete methods or fields? */
  def isTrait: Boolean = implClass ne NoClass

  /** Is this class a trait without concrete methods or a java interface? */
  def isInterface: Boolean = {
    ensureLoaded()
    ClassfileParser.isInterface(flags)
  }

  def isModule: Boolean = bytecodeName.endsWith("$")

  override def toString = "class " + bytecodeName

  def description: String = declarationPrefix + " " + formattedFullName
}
