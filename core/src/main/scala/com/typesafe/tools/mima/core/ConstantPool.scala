package com.typesafe.tools.mima.core

import scala.annotation.switch

import ClassfileConstants._

private[core] object ConstantPool {
  def parseNew(definitions: Definitions, in: BufferReader): ConstantPool = {
    val starts = new Array[Int](in.nextChar)
    var i = 1
    while (i < starts.length) {
      starts(i) = in.bp
      i += 1
      (in.nextByte.toInt: @switch) match {
        case CONSTANT_UTF8 | CONSTANT_UNICODE       => in.skip(in.nextChar)
        case CONSTANT_CLASS | CONSTANT_STRING
           | CONSTANT_METHODTYPE
           | CONSTANT_MODULE | CONSTANT_PACKAGE     => in.skip(2)
        case CONSTANT_METHODHANDLE                  => in.skip(3)
        case CONSTANT_INTEGER | CONSTANT_FLOAT
           | CONSTANT_FIELDREF | CONSTANT_METHODREF
           | CONSTANT_INTFMETHODREF
           | CONSTANT_NAMEANDTYPE
           | CONSTANT_INVOKEDYNAMIC                 => in.skip(4)
        case CONSTANT_LONG | CONSTANT_DOUBLE        => in.skip(8); i += 1
        case tag                                    => errorBadTag(tag, in.bp - 1)
      }
    }
    new ConstantPool(definitions, in, starts)
  }

  /** Throws an exception signaling a bad tag at given address. */
  private[ConstantPool] def errorBadTag(tag: Int, start: Int) =
    throw new RuntimeException(s"bad constant pool tag $tag at byte $start")
}

private[core]
final class ConstantPool private (definitions: Definitions, in: BytesReader, starts: Array[Int]) {
  import ClassInfo.ObjectClass

  private val length       = starts.length
  private val values       = new Array[AnyRef](length)
  private val internalized = new Array[String](length)

  /** Return the name found at given index. */
  def getName(index: Int): String = {
    indexedOrUpdate(values, index) {
      val start = getStart(index, CONSTANT_UTF8)
      in.getString(start + 3, in.getChar(start + 1))
    }
  }

  /** Return the name found at given index in the constant pool, with '/' replaced by '.'. */
  def getExternalName(index: Int): String = {
    indexedOrUpdate(internalized, index) {
      getName(index).replace('/', '.')
    }
  }

  /** Return the external name of the class info structure found at 'index'. */
  def getClassName(index: Int): String = {
    val start = getStart(index, CONSTANT_CLASS)
    getExternalName(in.getChar(start + 1))
  }

  def getClassInfo(index: Int): ClassInfo = {
    indexedOrUpdate(values, index) {
      definitions.fromName(getClassName(index))
    }
  }

  def getSuperClass(index: Int): ClassInfo = if (index == 0) ObjectClass else getClassInfo(index)

  private def indexedOrUpdate[A <: AnyRef, R <: A](arr: Array[A], index: Int)(mk: => R): R = {
    if (index <= 0 || index >= length)
      throw new RuntimeException(s"bad constant pool index: $index, length: $length")
    var value = arr(index).asInstanceOf[R]
    if (value eq null) {
      value = mk
      arr(index) = value
    }
    value
  }

  private def getStart(index: Int, expectedTag: Int) = {
    val start = starts(index)
    val tag = in.getByte(start).toInt
    if (tag == expectedTag) start
    else ConstantPool.errorBadTag(tag, start)
  }
}
