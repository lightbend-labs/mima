package ssol.tools.mima

import scala.tools.nsc.io.AbstractFile
import scala.reflect.NameTransformer

object ClassInfo {
  def formatClassName(str: String) = NameTransformer.decode(str).replace('$', '#')
  
  /** We assume there can be only one java.lang.Object class, and that comes from the configuration
   *  class path.
   */
  lazy val ObjectClass = Config.baseDefinitions.fromName("java.lang.Object")
}

/** A placeholder class info for a class that is not found on the classpath or in a given
 *  package.
 */
class SyntheticClassInfo(owner: PackageInfo, val name: String) extends ClassInfo(owner) {
  Config.info("<synthetic> " + toString)
  loaded = true
  def file: AbstractFile = throw new UnsupportedOperationException
}

/** As the name implies. */
object NoClass extends SyntheticClassInfo(null, "<noclass>") {
  override lazy val superClasses = List(this)
  override lazy val allTraits = Set(): Set[ClassInfo]
}

/** A class for which we have the classfile. */
class ConcreteClassInfo(owner: PackageInfo, val file: AbstractFile) extends ClassInfo(owner) {
  def name = PackageInfo.className(file.name)
}

abstract class ClassInfo(val owner: PackageInfo) {
  import ClassInfo._

  def file: AbstractFile

  def name: String
  
  def fullName: String = 
    if (owner.isRoot) name
    else owner.fullName + "." + name

  def classString = 
    if (name.endsWith("$")) "object "+formatClassName(fullName.init) 
    else if (isTrait || loaded && isInterface) "trait "+formatClassName(fullName)
    else "class "+formatClassName(fullName)

  protected var loaded = false
  private def ensureLoaded() =
    if (!loaded)
      try {
        Config.info("parsing " + file)
        owner.definitions.ClassfileParser.parse(this)
      } finally {
        loaded = true
      }
      
  private var _superClass: ClassInfo = NoClass
  private var _interfaces: List[ClassInfo] = List()
  private var _fields: Members = null
  private var _methods: Members = null
  private var _flags: Int = 0
  private var _isScala: Boolean = false

  def superClass: ClassInfo = { ensureLoaded(); _superClass }
  def interfaces: List[ClassInfo] = { ensureLoaded(); _interfaces }
  def fields: Members = { ensureLoaded(); _fields }
  def methods: Members = { ensureLoaded(); _methods }
  def flags: Int = _flags

  /** currently never set! */
  def isScala: Boolean = { ensureLoaded(); _isScala }

  def superClass_=(x: ClassInfo) = _superClass = x
  def interfaces_=(x: List[ClassInfo]) = _interfaces = x
  def fields_=(x: Members) = _fields = x
  def methods_=(x: Members) = _methods = x
  def flags_=(x: Int) = _flags = x
  def isScala_=(x: Boolean) = _isScala = x

  lazy val superClasses: List[ClassInfo] = 
    this :: (if (this == ClassInfo.ObjectClass) List() else superClass.superClasses)

  def lookupFields(name: String): Iterator[MemberInfo] =
    superClasses.iterator flatMap (_.fields.get(name))

  def lookupMethods(name: String): Iterator[MemberInfo] =
    superClasses.iterator flatMap (_.methods.get(name))

  /** Is this class a non-trait that inherits !from a trait */
  lazy val isClassInheritsTrait = !isInterface && _interfaces.exists(_.isTrait)

  /** Should methods be parsed from classfile? */
  def methodsAreRelevant = isTrait || isImplClass || _interfaces.exists(_.isTrait)

  /** The constructors of this class
   *  pre: methodsAreRelevant
   */
  def constructors: List[MemberInfo] = 
    if (methods == null) null
    else methods.iterator.filter(_.isClassConstructor).toList

  /** The setter methods defined of this trait that correspond to
   *  a concrete field. TODO: define and check annotation for a mutable
   *  setter.
   */
  lazy val traitSetters: List[MemberInfo] = {
    assert(isTrait)
    methods.iterator filter (_.isTraitSetter) toList
  }

  /** The concrete methods of this trait */
  lazy val concreteMethods: List[MemberInfo] = {
    assert(isTrait)
    methods.iterator filter (implClass.hasStaticImpl(_)) toList
  }

  /** The inherited traits in the linearization of this class or trait,
   *  except any traits inherited by its superclass.
   *  Traits appear in linearization order of this class or trait.
   */
  lazy val directTraits: List[ClassInfo] = parentsClosure(this)

  /** All traits inherited directly or indirectly by this class */ 
  lazy val allTraits: Set[ClassInfo] =
    if (this == ClassInfo.ObjectClass || this == NoClass) Set()
    else superClass.allTraits ++ directTraits

  /** All traits in the transitive, reflexive inheritance closure of given trait `t' */
  private def traitClosure(t: ClassInfo): List[ClassInfo] =  
    if (superClass.allTraits contains t) List()
    else if (t.isTrait) parentsClosure(t) :+ t
    else parentsClosure(t)

  def parentsClosure(c: ClassInfo) =
    (c.interfaces flatMap traitClosure).distinct

  private def unimplemented(sel: ClassInfo => Traversable[MemberInfo]): List[MemberInfo] = {
    ensureLoaded()
    if (isClassInheritsTrait) {
      for {
        t <- directTraits.toList
        m <- sel(t)
        if !hasInstanceImpl(m)
      } yield m
    }  else List()
  }
    
  /** The methods that should be implemented by this class but aren't */
  lazy val unimplementedMethods = unimplemented(_.concreteMethods)

  /** The fields that should be implemented by this class but aren't */
  lazy val unimplementedSetters = unimplemented(_.traitSetters) 

  /** Does this class have an implementation (forwarder or accessor) of given method `m'? */
  def hasInstanceImpl(m: MemberInfo) =
    methods.get(m.name) exists (_.sig == m.sig)

  /** Does this implementation class have a static implementation of given method `m'? */
  def hasStaticImpl(m: MemberInfo) = staticImpl(m).isDefined

  /** Optionally, the static implementation method corresponding to trait member `m' */
  def staticImpl(m: MemberInfo): Option[MemberInfo] = {
    assert(isImplClass, this)
    methods.get(m.name) find (im => hasImplSig(im.sig, m.sig))
  }

  /** Does `isig' correspond to `tsig' if seen as the signature of the static
   *  implementation method of a trait method with signature `tsig'?
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
  lazy val isImplClass: Boolean = name endsWith PackageInfo.implClassSuffix

  /** The implementation class corresponding to this trait */
  private var _implClass: ClassInfo = NoClass

  def implClass_=(ic: ClassInfo) = _implClass = ic

  /** The implementation class of this trait, or NoClass if it is not a trait.
   */
  def implClass: ClassInfo = {
    owner.traits // make sure we have implClass set
    _implClass
  }

  /** Is this class a trait with some concrete methods or fields? */
  def isTrait: Boolean = implClass ne NoClass

  /** Is this class a trait or interface? */
  def isInterface: Boolean = { 
    ensureLoaded()
    ClassfileParser.isInterface(flags) 
  }

  def isObject: Boolean = name.endsWith("$")

  /** Is this class public? */
  def isPublic: Boolean = { 
    ensureLoaded() 
    ClassfileParser.isPublic(flags) 
  }

  /** Is this class public? */
  def isPackageVisible: Boolean = {
    ensureLoaded() 
    !ClassfileParser.isPrivate(flags)
  }

  override def toString = "class "+name

  def description: String = (if (isTrait) "trait " else "class ") + fullName
}
