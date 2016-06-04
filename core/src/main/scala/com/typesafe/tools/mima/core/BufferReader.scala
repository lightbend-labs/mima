/* NSC -- new Scala compiler
 * Copyright 2005-2010 LAMP/EPFL
 * @author  Martin Odersky
 */


package com.typesafe.tools.mima.core

import java.lang.Float.intBitsToFloat
import java.lang.Double.longBitsToDouble

/**
 * This class reads files byte per byte. Only used by ClassFileParser
 *
 * @author Philippe Altherr
 * @version 1.0, 23/03/2004
 */
class BufferReader(val buffer: Array[Byte]) {

  /** the current input pointer
   */
  private var bp: Int = 0

  def position: Int = bp

  def setPosition(pos: Int): Unit = {
    rangeCheck(pos)
    bp = pos
  }

  private def rangeCheck(pos: Int): Unit = {
    if (pos < 0) {
      throw new IndexOutOfBoundsException("Illegal index " + pos)
    }

    if (pos >= buffer.length) {
      throw new IndexOutOfBoundsException("Position " + pos + " is larger than array size " + buffer.length)
    }
  }

  private def buf(pos: Int) = {
    rangeCheck(pos)
    buffer(pos)
  }

  /** return byte at offset 'pos'
   */
  @throws(classOf[IndexOutOfBoundsException])
  def byteAt(pos: Int): Byte = buf(pos)

  /** read a byte
   */
  @throws(classOf[IndexOutOfBoundsException])
  def nextByte: Byte = {
    val b = buf(bp)
    bp += 1
    b
  }

  /** read some bytes
   */
  def nextBytes(len: Int): Array[Byte] = {
    if (bp + len >= buffer.length) {
      throw new IndexOutOfBoundsException("Length " + len + " plus bp " + bp + " is more than array size " + buffer.length)
    }
    val b = buffer.slice(bp, bp + len)
    bp += len
    b
  }

  /** read a character
   */
  def nextChar: Char =
    (((nextByte & 0xff) << 8) + (nextByte & 0xff)).toChar

  /** read an integer
   */
  def nextInt: Int =
    ((nextByte & 0xff) << 24) + ((nextByte & 0xff) << 16) +
    ((nextByte & 0xff) <<  8) +  (nextByte & 0xff)


  /** extract a character at position bp from buf
   */
  def getChar(mybp: Int): Char =
    (((buf(mybp) & 0xff) << 8) + (buf(mybp+1) & 0xff)).toChar

  /** extract an integer at position bp from buf
   */
  def getInt(mybp: Int): Int =
    ((buf(mybp  ) & 0xff) << 24) + ((buf(mybp+1) & 0xff) << 16) +
    ((buf(mybp+2) & 0xff) << 8) + (buf(mybp+3) & 0xff)

  /** extract a long integer at position bp from buf
   */
  def getLong(mybp: Int): Long =
    (getInt(mybp).toLong << 32) + (getInt(mybp + 4) & 0xffffffffL)

  /** extract a float at position bp from buf
   */
  def getFloat(mybp: Int): Float = intBitsToFloat(getInt(mybp))

  /** extract a double at position bp from buf
   */
  def getDouble(mybp: Int): Double = longBitsToDouble(getLong(mybp))

  /** skip next 'n' bytes
   */
  def skip(n: Int) { bp += n }

  /** Do read operattion `op` at position `n`
   */
  def at[T](n: Int)(op: => T): T = {
    if (n >= buffer.length) {
      throw new IndexOutOfBoundsException("input " + n + " more than buffer length " + buffer.length) 
    }
    val oldbp = bp
    bp = n
    try {
      op
    } finally {
      bp = oldbp
    }
  }
}
