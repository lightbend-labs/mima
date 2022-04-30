package com.typesafe.tools.mima.core

import java.lang.Float.intBitsToFloat
import java.lang.Double.longBitsToDouble
import java.nio.charset.StandardCharsets

/** Reads and interprets the bytes in a class file byte-buffer. */
sealed abstract private[core] class BytesReader(buf: Array[Byte]) {
  def pos: Int
  def file: AbsFile

  final def getByte(idx: Int): Byte = buf(idx)
  final def getChar(idx: Int): Char = (((buf(idx) & 0xff) << 8) + (buf(idx + 1) & 0xff)).toChar

  final def getInt(idx: Int): Int =
    ((buf(idx) & 0xff) << 24) + ((buf(idx + 1) & 0xff) << 16) +
      ((buf(idx + 2) & 0xff) << 8) + (buf(idx + 3) & 0xff)

  final def getLong(idx: Int): Long     = (getInt(idx).toLong << 32) + (getInt(idx + 4) & 0xffffffffL)
  final def getFloat(idx: Int): Float   = intBitsToFloat(getInt(idx))
  final def getDouble(idx: Int): Double = longBitsToDouble(getLong(idx))

//final def getString(idx: Int, len: Int): String = new java.io.DataInputStream(new java.io.ByteArrayInputStream(buf, idx, len)).readUTF
  final def getString(idx: Int, len: Int): String = new String(buf, idx, len, StandardCharsets.UTF_8)

  final def getBytes(idx: Int, bytes: Array[Byte]): Unit = System.arraycopy(buf, idx, bytes, 0, bytes.length)
}

/** A BytesReader which also holds a mutable pointer to where it will read next. */
final private[core] class BufferReader(val file: AbsFile) extends BytesReader(file.toByteArray) {

  /** the buffer pointer */
  var bp: Int = 0
  def pos     = bp

  def nextByte: Byte = { val b = getByte(bp); bp += 1; b }
  def nextChar: Char = { val c = getChar(bp); bp += 2; c } // Char = unsigned 2-bytes, aka u16
  def nextInt: Int   = { val i = getInt(bp); bp += 4; i }

  def acceptByte(exp: Byte, ctx: => String = "") = {
    val obt = nextByte; assert(obt == exp, s"Expected $exp, obtained $obt$ctx"); obt
  }
  def acceptChar(exp: Char, ctx: => String = "") = {
    val obt = nextChar; assert(obt == exp, s"Expected $exp, obtained $obt$ctx"); obt
  }

  def skip(n: Int): Unit = bp += n

  def atIndex[T](i: Int)(body: => T): T = {
    val saved = bp
    bp = i
    try body
    finally bp = saved
  }
}
