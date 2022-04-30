package com.typesafe.tools.mima.core

final class PickleBuffer(val bytes: Array[Byte]) {
  var readIndex = 0

  def readByte(): Int = { val x = bytes(readIndex).toInt; readIndex += 1; x }
  def nextByte(): Int = bytes(readIndex + 1).toInt

  /** Read a natural number in big endian format, base 128. All but the last digits have bit 0x80 set. */
  def readNat(): Int = readLongNat().toInt

  def readLongNat(): Long = {
    var b = 0L
    var x = 0L
    while ({
      b = readByte().toLong
      x = (x << 7) + (b & 0x7f)
      (b & 0x80) != 0L
    }) ()
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

  /**
   * The indices in the bytes array where each consecutive entry starts. The length of the array is the number of
   * entries in the pickle bytes.
   */
  def createIndex: Array[Int] =
    atIndex(0) {
      readNat(); readNat() // discard version
      0.until(readNat())
        .iterator
        .map { _ =>
          val idx = readIndex
          readByte() // tag
          val len = readNat()
          readIndex += len
          idx
        }
        .toArray
    }

  def atIndex[T](i: Int)(body: => T): T = {
    val saved = readIndex
    readIndex = i
    try body
    finally readIndex = saved
  }

  def assertEnd(end: Int) = assert(readIndex == end, s"Expected at end=$end but readIndex=$readIndex")
}
