package com.typesafe.tools.mima.core

final class PickleBuffer(val bytes: Array[Byte]) {
  var readIndex = 0

  def readByte(): Int = { val x = bytes(readIndex).toInt; readIndex += 1; x }

  /** Read a natural number in big endian format, base 128. All but the last digits have bit 0x80 set. */
  def readNat(): Int = readLongNat().toInt

  def readLongNat(): Long = {
    var b = 0L
    var x = 0L
    do {
      b = readByte().toLong
      x = (x << 7) + (b & 0x7f)
    } while ((b & 0x80) != 0L)
    x
  }

  /** Read a long number in signed big endian format, base 256. */
  def readLong(len: Int): Long = {
    var x = 0L
    var i = 0
    while (i < len) {
      x = (x << 8) + (readByte() & 0xff)
      i += 1
    }
    val leading = 64 - (len << 3)
    x << leading >> leading
  }

  def createIndex: Array[Int] = {
    atIndex(0) {
      readNat(); readNat() // discard version
      0.until(readNat()).iterator.map { _ =>
        val idx = readIndex
        readByte() // tag
        val len = readNat()
        readIndex += len
        idx
      }.toArray
    }
  }

  /** Returns the buffer as a sequence of (Int, Array[Byte]) representing
   *  (tag, data) of the individual entries.  Saves and restores buffer state.
   */
  def toIndexedSeq: IndexedSeq[(Int, Array[Byte])] = {
    for (idx <- createIndex) yield {
      atIndex(idx) {
        val tag = readByte()
        val len = readNat()
        tag -> bytes.slice(readIndex, readIndex + len)
      }
    }
  }

  def atIndex[T](i: Int)(body: => T): T = {
    val saved = readIndex
    readIndex = i
    try body finally readIndex = saved
  }
}
