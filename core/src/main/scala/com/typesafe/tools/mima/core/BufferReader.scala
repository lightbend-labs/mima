package com.typesafe.tools.mima.core

import java.lang.Float.intBitsToFloat
import java.lang.Double.longBitsToDouble
import java.nio.charset.StandardCharsets

/** Reads and interprets the bytes in a class file byte-buffer. */
private[core] sealed class BytesReader(buf: Array[Byte]) {
  final def getByte(idx: Int): Byte = buf(idx)
  final def getChar(idx: Int): Char = (((buf(idx) & 0xff) << 8) + (buf(idx + 1) & 0xff)).toChar

  final def getInt(idx: Int): Int =
    ((buf(idx    ) & 0xff) << 24) + ((buf(idx + 1) & 0xff) << 16) +
    ((buf(idx + 2) & 0xff) << 8)  +  (buf(idx + 3) & 0xff)

  final def getLong(idx: Int): Long     = (getInt(idx).toLong << 32) + (getInt(idx + 4) & 0xffffffffL)
  final def getFloat(idx: Int): Float   = intBitsToFloat(getInt(idx))
  final def getDouble(idx: Int): Double = longBitsToDouble(getLong(idx))

  final def getString(idx: Int, len: Int): String = new String(buf, idx, len, StandardCharsets.UTF_8)
}

/** A BytesReader which also holds a mutable pointer to where it will read next. */
private[core] final class BufferReader(buf: Array[Byte]) extends BytesReader(buf) {
  /** the buffer pointer */
  var bp: Int = 0

  def nextByte: Byte = { val b = getByte(bp); bp += 1; b }
  def nextChar: Char = { val c = getChar(bp); bp += 2; c } // Char = unsigned 2-bytes, aka u16
  def nextInt: Int   = { val i = getInt(bp);  bp += 4; i }

  def skip(n: Int): Unit = bp += n
}
