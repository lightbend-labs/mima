package com.typesafe.tools.mima.core

import java.lang.Float.intBitsToFloat
import java.lang.Double.longBitsToDouble
import java.nio.charset.StandardCharsets.UTF_8

/** Immutable reader of a classfile byte-buffer. */
private[core] sealed class BytesReader(buf: Array[Byte]) {
  final private def u16(idx: Int): Int = (getByte(idx) & 0xff << 8) + getByte(idx + 1) & 0xff

  final def getByte(idx: Int): Byte               = buf(idx)
  final def getChar(idx: Int): Char               = u16(idx).toChar
  final def getInt(idx: Int): Int                 = (u16(idx) << 16) + u16(idx + 2)
  final def getLong(idx: Int): Long               = (getInt(idx).toLong << 32) + (getInt(idx + 4) & 0xffffffffL)
  final def getFloat(idx: Int): Float             = intBitsToFloat(getInt(idx))
  final def getDouble(idx: Int): Double           = longBitsToDouble(getLong(idx))
  final def getString(idx: Int, len: Int): String = new String(buf, idx, len, UTF_8)
}

/** A BytesReader which also holds a mutable pointer to where it will read next. */
private[core] final class BufferReader(buf: Array[Byte]) extends BytesReader(buf) {
  /** the buffer pointer */
  var bp: Int = 0

  def nextByte: Byte = { val b = getByte(bp); bp += 1; b } // 1 byte  ( 8   signed bits, i8)
  def nextChar: Char = { val c = getChar(bp); bp += 2; c } // 2 bytes (16 unsigned bits, u16)
  def nextInt: Int   = { val i = getInt(bp);  bp += 4; i } // 4 bytes (32   signed bits, i32)

  def skip(n: Int): Unit = bp += n
}
