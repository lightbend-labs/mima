package com.typesafe.tools.mima.core

import scala.annotation.switch

import scala.tools.nsc.symtab.classfile.ClassfileConstants._

private[core] final class ConstantPool(definitions: Definitions, in: BufferReader) {
  import ClassfileParser._

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
        case CONSTANT_CLASS | CONSTANT_STRING | CONSTANT_METHODTYPE
           | CONSTANT_MODULE | CONSTANT_PACKAGE =>
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
      if (in.getByte(start).toInt != CONSTANT_UTF8) errorBadTag(start)
      name = in.getString(start + 3, in.getChar(start + 1))
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
      if (in.getByte(start).toInt != CONSTANT_CLASS) errorBadTag(start)
      val name = getExternalName(in.getChar(start + 1))
      c = definitions.fromName(name)
      values(index) = c
    }
    c
  }

  /** Return the external name of the class info structure found at 'index'.
   *  Use 'getClassSymbol' if the class is sure to be a top-level class.
   */
  def getClassName(index: Int): String = {
    val start = starts(index)
    if (in.getByte(start).toInt != CONSTANT_CLASS) errorBadTag(start)
    getExternalName(in.getChar(start + 1))
  }

  def getSuperClass(index: Int): ClassInfo =
    if (index == 0) ClassInfo.ObjectClass else getClassInfo(index)

  /** Throws an exception signaling a bad constant index. */
  private def errorBadIndex(index: Int) =
    throw new RuntimeException("bad constant pool index: " + index + " at pos: " + in.bp)

  /** Throws an exception signaling a bad tag at given address. */
  private def errorBadTag(start: Int) =
    throw new RuntimeException("bad constant pool tag " + in.getByte(start) + " at byte " + start)
}
