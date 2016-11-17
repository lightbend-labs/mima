/* NSC -- new Scala compiler
 * Copyright 2005-2010 LAMP/EPFL
 * @author  Martin Odersky
 */

package com.typesafe.tools.mima.core

import java.io.IOException
import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.symtab.classfile.ClassfileConstants._
import scala.annotation.switch

class ClientClassfileParser(definitions: Definitions) extends ClassfileParser(definitions) {
  var readFields = (clazz: ClassInfo) => false
  var readMethods = (clazz: ClassInfo) => clazz.methodsAreRelevant
  var readCode = (meth: MemberInfo) => meth.needCode
}

class LibClassfileParser(definitions: Definitions) extends ClassfileParser(definitions) {
  var readFields = (clazz: ClassInfo) => true
  var readMethods = (clazz: ClassInfo) => true
  var readCode = (meth: MemberInfo) => false
}


/** This abstract class implements a class file parser.
 *
 *  @author Martin Odersky
 *  @version 1.0
 */
abstract class ClassfileParser(definitions: Definitions) {
  import ClassfileParser._

  /** Configuration:
   */
  def readFields: ClassInfo => Boolean
  def readMethods: ClassInfo => Boolean
  def readCode: MemberInfo => Boolean

  var in: BufferReader = _  // the class file reader
  private var thepool: ConstantPool = _
  protected var parsedClass: ClassInfo = _

  def pool: ConstantPool = thepool

  def parse(clazz: ClassInfo) = synchronized {
    parsed += 1
    parsedClass = clazz
    in = new BufferReader(clazz.file.toByteArray)
    parseAll(clazz)
  }

  protected def parseAll(clazz: ClassInfo) {
    parseHeader()
    thepool = new ConstantPool
    parseClass(clazz)
  }

  protected def parseHeader() {
    val magic = in.nextInt
    if (magic != JAVA_MAGIC)
      throw new IOException("class file '" + parsedClass.file + "' "
                            + "has wrong magic number 0x" + magic.toHexString
                            + ", should be 0x" + JAVA_MAGIC.toHexString)
    val minorVersion = in.nextChar.toInt
    val majorVersion = in.nextChar.toInt
  }

  class ConstantPool {
    val length = in.nextChar
    private val starts = new Array[Int](length)
    private val values = new Array[AnyRef](length)
    private val internalized = new Array[String](length)

    { var i = 1
      while (i < length) {
        starts(i) = in.bp
        i += 1
        (in.nextByte.toInt: @switch) match {
          case CONSTANT_UTF8 | CONSTANT_UNICODE =>
            in.skip(in.nextChar)
          case CONSTANT_CLASS | CONSTANT_STRING | CONSTANT_METHODTYPE =>
            in.skip(2)
          case CONSTANT_METHODHANDLE =>
            in.skip(3)
          case CONSTANT_FIELDREF | CONSTANT_METHODREF | CONSTANT_INTFMETHODREF
             | CONSTANT_NAMEANDTYPE | CONSTANT_INTEGER | CONSTANT_FLOAT
             | CONSTANT_INVOKEDYNAMIC =>
            in.skip(4)
          case CONSTANT_LONG | CONSTANT_DOUBLE =>
            in.skip(8)
            i += 1
          case _ =>
            errorBadTag(in.bp - 1)
        }
      }
    }

    /** Return the name found at given index. */
    def getName(index: Int): String = {
      if (index <= 0 || length <= index) errorBadIndex(index)
      var name = values(index).asInstanceOf[String]
      if (name eq null) {
        val start = starts(index)
        if (in.buf(start).toInt != CONSTANT_UTF8) errorBadTag(start)
        name = UTF8Codec.decode(in.buf, start + 3, in.getChar(start + 1))
        values(index) = name
      }
      name
    }

    /** Return the name found at given index in the constant pool, with '/' replaced by '.'. */
    def getExternalName(index: Int): String = {
      if (index <= 0 || length <= index) errorBadIndex(index)
      if (internalized(index) eq null) {
        internalized(index) = getName(index).replace('/', '.')
      }
      internalized(index)
    }

    def getClassInfo(index: Int): ClassInfo = {
      if (index <= 0 || length <= index) errorBadIndex(index)
      var c = values(index).asInstanceOf[ClassInfo]
      if (c eq null) {
        val start = starts(index)
        if (in.buf(start).toInt != CONSTANT_CLASS) errorBadTag(start)
        val name = getExternalName(in.getChar(start + 1))
        c = definitions.fromName(name)
        //if (c == ClassInfo.NoClass) println("warning: missing class "+name+" referenced from "+parsedClass.file)
        values(index) = c
      }
      c
    }

    /** Return the external name of the class info structure found at 'index'.
     *  Use 'getClassSymbol' if the class is sure to be a top-level class.
     */
    def getClassName(index: Int): String = {
      val start = starts(index)
      if (in.buf(start).toInt != CONSTANT_CLASS) errorBadTag(start)
      getExternalName(in.getChar(start + 1))
    }

    def getSuperClass(index: Int): ClassInfo =
      if (index == 0) ClassInfo.ObjectClass else getClassInfo(index)

    /** Return a name string and a type string at the given index. If the type is a method
     *  type, a dummy symbol is created in 'ownerTpe', which is used as the
     *  owner of its value parameters. This might lead to inconsistencies,
     *  if a symbol of the given name already exists, and has a different
     *  type.
     */
    def getNameAndType(index: Int): (String, String) = {
      if (index <= 0 || length <= index) errorBadIndex(index)
      var p = values(index).asInstanceOf[(String, String)]
      if (p eq null) {
        val start = starts(index)
        if (in.buf(start).toInt != CONSTANT_NAMEANDTYPE) errorBadTag(start)
        val name = getName(in.getChar(start + 1).toInt)
        var tpe  = getName(in.getChar(start + 3).toInt)
        p = (name, tpe)
      }
      p
    }

    /** Return a triple consisting of class info, name string, and type string
     *  at the given index. If the class is an array or is not found on the classpath
     *  NoClass is returned.
     */
    def getReference(index: Int): Reference = {
      if (index <= 0 || length <= index) errorBadIndex(index)
      var r = values(index).asInstanceOf[Reference]
      if (r eq null) {
        val start = starts(index)
        val first = in.buf(start).toInt
        if (first != CONSTANT_FIELDREF &&
            first != CONSTANT_METHODREF &&
            first != CONSTANT_INTFMETHODREF) errorBadTag(start)
        val clazz = getClassInfo(in.getChar(start + 1))
        val (name, tpe) = getNameAndType(in.getChar(start + 3))
        r = Reference(clazz, name, tpe)
      }
      r
    }

    /** Throws an exception signaling a bad constant index. */
    private def errorBadIndex(index: Int) =
      throw new RuntimeException("bad constant pool index: " + index + " at pos: " + in.bp)

    /** Throws an exception signaling a bad tag at given address. */
    private def errorBadTag(start: Int) =
      throw new RuntimeException("bad constant pool tag " + in.buf(start) + " at byte " + start)
  }

  def parseMembers(clazz: ClassInfo): Members = {
    val memberCount = in.nextChar
    var members = new ArrayBuffer[MemberInfo]
    for (i <- 0 until memberCount) {
      val jflags = in.nextChar.toInt
      if (isPrivate(jflags)) {
        in.skip(4)
        skipAttributes()
      } else {
        members += parseMember(clazz, jflags)
      }
    }
    new Members(members)
  }

  def skipMembers(): Members = {
    val memberCount = in.nextChar
    for (i <- 0 until memberCount) {
      in.skip(6); skipAttributes()
    }
    NoMembers
  }

  def parseMember(clazz: ClassInfo, jflags: Int): MemberInfo = {
    val name = pool.getName(in.nextChar)
    val sig = pool.getExternalName(in.nextChar)
    val result = new MemberInfo(clazz, name, jflags, sig)
    parseAttributes(result)
    result
  }

  def parseClass(clazz: ClassInfo) {
    clazz.flags = in.nextChar
    val nameIdx = in.nextChar
    val externalName = pool.getClassName(nameIdx)

    def parseSuperClass(): ClassInfo =
      if (hasAnnotation(clazz.flags)) { in.nextChar; definitions.AnnotationClass }
      else pool.getSuperClass(in.nextChar)

    def parseInterfaces(): List[ClassInfo] = {
      val rawInterfaces =
        for (i <- List.range(0, in.nextChar)) yield pool.getSuperClass(in.nextChar)
      rawInterfaces filter (_ != NoClass)
    }

    clazz.superClass = parseSuperClass()
    clazz.interfaces = parseInterfaces()
    if (readFields(clazz)) clazz.fields = parseMembers(clazz) else skipMembers()
    val methods =
      if (readMethods(clazz)) parseMembers(clazz) else skipMembers()
    parseAttributes(clazz)
    clazz.methods =
      if (clazz.isScalaUnsafe && !clazz.isImplClass) methods.withoutStatic else methods
  }

  def skipAttributes() {
    val attrCount = in.nextChar
    for (i <- 0 until attrCount) {
      in.skip(2); in.skip(in.nextInt)
    }
  }

  def parseAttributes(c: ClassInfo) {
    val attrCount = in.nextChar
     for (i <- 0 until attrCount) {
       val attrIndex = in.nextChar
       val attrName = pool.getName(attrIndex)
       val attrLen = in.nextInt
       val attrEnd = in.bp + attrLen
       if (attrName == "SourceFile") {
         if (in.bp + 1 <= attrEnd) {
           val attrNameIndex = in.nextChar
           c.sourceFileName = pool.getName(attrNameIndex)
         }
       } else if (attrName == "Scala" || attrName == "ScalaSig") {
         this.parsedClass.isScala = true
       }
       in.bp = attrEnd
     }
  }

  /** Return true iff TraitSetter annotation found among attributes */
  def parseAttributes(m: MemberInfo) {
    val maybeTraitSetter = MemberInfo.maybeSetter(m.bytecodeName)
    val attrCount = in.nextChar
    for (i <- 0 until attrCount) {
      val attrIndex = in.nextChar
      val attrName = pool.getName(attrIndex)
      val attrLen = in.nextInt
      val attrEnd = in.bp + attrLen
      if (maybeTraitSetter && attrName == "RuntimeVisibleAnnotations") {
        val annotCount = in.nextChar
        var j = 0
        while (j < annotCount && !m.isTraitSetter) {
          if (in.bp + 2 <= attrEnd) {
            val annotIndex = in.nextChar
            if (pool.getName(annotIndex) == "Lscala/runtime/TraitSetter;")
              m.isTraitSetter = true
            else
              skipAnnotation(annotIndex, attrEnd)
          }
          j += 1
        }
      } else if (attrName == "Code" && readCode(m)) {
        val maxStack = in.nextChar
        val maxLocals = in.nextChar
        val codeLength = in.nextInt
        m.codeOpt = Some((in.bp, in.bp + codeLength))
      } else if (attrName == "Deprecated") {
        m.isDeprecated = true
      }
      in.bp = attrEnd
    }
  }

  /** Skip a single annotation
   */
  def skipAnnotation(annotIndex: Int, attrEnd: Int) {
    try {
      if (in.bp + 2 <= attrEnd) {
        val nargs = in.nextChar
        for (i <- 0 until nargs)
          if (in.bp + 2 <= attrEnd) {
            val argname = in.nextChar
            skipAnnotArg(attrEnd)
          }
      }
    } catch {
      case ex: Exception =>
    }
  }

  /** Skip a single annotation argument
   */
  def skipAnnotArg(attrEnd: Int) {
    if (in.bp + 3 <= attrEnd) {
      val tag = in.nextByte.toChar
      val index = in.nextChar
      tag match {
        case ENUM_TAG   =>
          if (in.bp + 2 <= attrEnd) in.nextChar
        case ARRAY_TAG  =>
          for (i <- 0 until index)
            skipAnnotArg(attrEnd)
        case ANNOTATION_TAG =>
          skipAnnotation(index, attrEnd)
        case _ =>
      }
    }
  }
}

object ClassfileParser {
  var parsed: Int = 0

  @inline final def isPublic(flags: Int) =
    (flags & JAVA_ACC_PUBLIC) != 0
  @inline final def isProtected(flags: Int) =
    (flags & JAVA_ACC_PROTECTED) != 0
  @inline final def isPrivate(flags: Int) =
    (flags & JAVA_ACC_PRIVATE) != 0
  @inline final def isStatic(flags: Int) =
    (flags & JAVA_ACC_STATIC) != 0
  @inline final private def hasAnnotation(flags: Int) =
    (flags & JAVA_ACC_ANNOTATION) != 0
  @inline final def isInterface(flags: Int) =
    (flags & JAVA_ACC_INTERFACE) != 0
  @inline final def isDeferred(flags: Int) =
    (flags & JAVA_ACC_ABSTRACT) != 0
  @inline final def isFinal(flags: Int) =
    (flags & JAVA_ACC_FINAL) != 0
  @inline final def isSynthetic(flags: Int) =
    (flags & JAVA_ACC_SYNTHETIC) != 0
  @inline final def isBridge(flags: Int) =
    (flags & JAVA_ACC_BRIDGE) != 0
}
