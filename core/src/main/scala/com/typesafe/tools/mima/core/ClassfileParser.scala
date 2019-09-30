package com.typesafe.tools.mima.core

import java.io.IOException

import scala.reflect.io.AbstractFile
import scala.tools.nsc.symtab.classfile.ClassfileConstants._

final class ClassfileParser private (in: BufferReader, pool: ConstantPool) {
  import ClassfileParser._

  private def parseClass(clazz: ClassInfo): Unit = {
    val flags = in.nextChar
    clazz._flags = flags
    in.skip(2) // external name index

    clazz._superClass = parseSuperClass(clazz, flags)
    clazz._interfaces = parseInterfaces()
    clazz._fields     = parseMembers[FieldInfo](clazz)
    clazz._methods    = parseMembers[MethodInfo](clazz)
    parseAttributes(clazz)
  }

  private def parseSuperClass(clazz: ClassInfo, flags: Int): ClassInfo = {
    if (isAnnotation(flags)) {
      in.skip(2)
      clazz.owner.definitions.AnnotationClass
    } else pool.getSuperClass(in.nextChar)
  }

  private def parseInterfaces(): List[ClassInfo] = {
    List.fill(in.nextChar)(pool.getSuperClass(in.nextChar))
  }

  private def parseMembers[A <: MemberInfo : MkMember](clazz: ClassInfo): Members[A] = {
    val members = {
      for {
        _ <- 0.until(in.nextChar).iterator
        flags = in.nextChar
        if !isPrivate(flags) || {
          in.skip(4)
          for (_ <- 0 until in.nextChar) {
            in.skip(2)
            in.skip(in.nextInt)
          }
          false
        }
      } yield parseMember[A](clazz, flags)
    }.toList
    new Members[A](members)
  }

  private def parseMember[A <: MemberInfo : MkMember](clazz: ClassInfo, flags: Int): A = {
    val name = pool.getName(in.nextChar)
    val descriptor = pool.getExternalName(in.nextChar)
    val memberInfo = implicitly[MkMember[A]].make(clazz, name, flags, descriptor)
    parseAttributes(memberInfo)
    memberInfo
  }

  private def parseAttributes(c: ClassInfo): Unit = {
    for (_ <- 0 until in.nextChar) {
      val attrIndex = in.nextChar
      val attrLen = in.nextInt
      val attrEnd = in.bp + attrLen
      pool.getName(attrIndex) match {
        case "EnclosingMethod" => c._isLocalClass = true
        case "InnerClasses"    => c._innerClasses = for {
          _ <- 0 until in.nextChar
          (innerIndex, outerIndex, innerNameIndex) = (in.nextChar, in.nextChar, in.nextChar)
          _ = in.skip(2)
          if innerIndex != 0 && outerIndex != 0 && innerNameIndex != 0
          className = pool.getClassName(innerIndex)
          // an inner class lists itself in InnerClasses
          _ = if (className == c.bytecodeName) c._isTopLevel = false
          if pool.getClassName(outerIndex) == c.bytecodeName
        } yield className
        case _                 => ()
      }
      in.bp = attrEnd
    }
  }

  private def parseAttributes(m: MemberInfo): Unit = {
    for (_ <- 0 until in.nextChar) {
      val attrIndex = in.nextChar
      val attrLen = in.nextInt
      val attrEnd = in.bp + attrLen
      pool.getName(attrIndex) match {
        case "Deprecated" => m.isDeprecated = true
        case "Signature"  => m.signature = pool.getName(in.nextChar)
        case _            => ()
      }
      in.bp = attrEnd
    }
  }
}

object ClassfileParser {
  private[core] def parseInPlace(clazz: ClassInfo, file: AbstractFile): Unit = {
    val in = new BufferReader(file.toByteArray)
    parseHeader(in, file.toString())
    val pool = ConstantPool.parseNew(clazz.owner.definitions, in)
    val parser = new ClassfileParser(in, pool)
    parser.parseClass(clazz)
  }

  @inline def isPublic(flags: Int)     = 0 != (flags & JAVA_ACC_PUBLIC)
  @inline def isPrivate(flags: Int)    = 0 != (flags & JAVA_ACC_PRIVATE)
  @inline def isProtected(flags: Int)  = 0 != (flags & JAVA_ACC_PROTECTED)
  @inline def isStatic(flags: Int)     = 0 != (flags & JAVA_ACC_STATIC)
  @inline def isFinal(flags: Int)      = 0 != (flags & JAVA_ACC_FINAL)
  @inline def isInterface(flags: Int)  = 0 != (flags & JAVA_ACC_INTERFACE)
  @inline def isDeferred(flags: Int)   = 0 != (flags & JAVA_ACC_ABSTRACT)
  @inline def isSynthetic(flags: Int)  = 0 != (flags & JAVA_ACC_SYNTHETIC)
  @inline def isAnnotation(flags: Int) = 0 != (flags & JAVA_ACC_ANNOTATION)

  private def parseHeader(in: BufferReader, file: String) = {
    val magic = in.nextInt
    if (magic != JAVA_MAGIC)
      throw new IOException(
        s"class file '$file' has wrong magic number 0x${magic.toHexString}, " +
            s"should be 0x${JAVA_MAGIC.toHexString}")
    in.skip(2) // minorVersion
    in.skip(2) // majorVersion
  }

  private trait MkMember[A] {
    def make(owner: ClassInfo, bytecodeName: String, flags: Int, descriptor: String): A
  }

  private implicit def mkFieldInfo: MkMember[FieldInfo]   = new FieldInfo(_, _, _, _)
  private implicit def mkMethodInfo: MkMember[MethodInfo] = new MethodInfo(_, _, _, _)
}
