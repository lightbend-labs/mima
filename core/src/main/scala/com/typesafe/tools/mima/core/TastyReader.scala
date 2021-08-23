package com.typesafe.tools.mima.core

import scala.collection.mutable.ListBuffer

import TastyRefs._

final class TastyReader(val bytes: Array[Byte], start: Int, end: Int, val base: Int = 0) {
  def this(bytes: Array[Byte]) = this(bytes, 0, bytes.length)

  private[this] var bp: Int = start

  def fork                = new TastyReader(bytes, bp, end, base)
  def forkAt(start: Addr) = new TastyReader(bytes, index(start), end, base)

  def addr(idx: Int): Addr   = Addr(idx - base)
  def index(addr: Addr): Int = addr.index + base
  def startAddr: Addr        = addr(start)          // The address of the first byte to read, respectively byte that was read
  def currentAddr: Addr      = addr(bp)             // The address of the next byte to read
  def endAddr: Addr          = addr(end)            // The address one greater than the last brte to read
  def isAtEnd: Boolean       = bp == end            // Have all bytes been read?
  def nextByte: Int          = bytes(bp) & 0xff     // Returns the next byte of data as a natural number without advancing the read position
  def readNat(): Int         = readLongNat().toInt  // Read a  natural number fitting in an Int in big endian format, base 128. All but the last digits have bit 0x80 set.
  def readInt(): Int         = readLongInt().toInt  // Read an integer number    in 2's complement big endian format, base 128. All but the last digits have bit 0x80 set.
  def readAddr(): Addr       = Addr(readNat())      // Read a natural number and return as an address
  def readEnd(): Addr        = addr(readNat() + bp) // Read a length number and return the absolute end address implied by it, given as (address following length field) + (length-value-read)
  def goto(addr: Addr): Unit = bp = index(addr)     // Set read position to the one pointed to by `addr`
  def readByte(): Int        = { val b = nextByte; bp += 1; b } // Read a byte of data

  /** Read the next `n` bytes of `data`. */
  def readBytes(n: Int): Array[Byte] = {
    val result = new Array[Byte](n)
    System.arraycopy(bytes, bp, result, 0, n)
    bp += n
    result
  }

  /** Read a natural number fitting in a Long in big endian format, base 128. All but the last digits have bit 0x80 set. */
  def readLongNat(): Long = {
    var b = 0L
    var x = 0L
    while ({
      b = bytes(bp)
      x = (x << 7) | (b & 0x7f)
      bp += 1
      (b & 0x80) == 0
    }) ()
    x
  }

  /** Read a long integer number in 2's complement big endian format, base 128. */
  def readLongInt(): Long = {
    var b = bytes(bp)
    var x: Long = (b << 1).toByte >> 1 // sign extend with bit 6.
    bp += 1
    while ((b & 0x80) == 0) {
      b = bytes(bp)
      x = (x << 7) | (b & 0x7f)
      bp += 1
    }
    x
  }

  /** Read an uncompressed Long stored in 8 bytes in big endian format. */
  def readUncompressedLong(): Long = {
    var x = 0L
    for (_ <- 0 to 7)
      x = (x << 8) | readByte()
    x
  }

  /** Perform `op` until `end` address is reached and collect results in a list. */
  def until[T](end: Addr)(op: => T): List[T] = {
    val buf = new ListBuffer[T]
    doUntil(end)(buf += op)
    buf.toList
  }

  def doUntil(end: Addr)(op: => Unit): Unit = {
    val endIdx = index(end)
    while (bp < endIdx) op
    assertEnd(end)
  }

  def assertEnd(end: Addr, info: String = "") = {
    assert(bp == index(end), s"incomplete read: curr=$currentAddr != end=$end$info")
  }

  def softAssertEnd(end: Addr, info: String = "") = {
    assertSoft(currentAddr == end, s"incomplete read: curr=$currentAddr != end=$end$info")
    goto(end)
  }

  def assertSoft(cond: Boolean, msg: => String)  = if (!cond) println(msg)
  def assertShort(cond: Boolean, msg: => String) = if (!cond) shortExc(s"$msg")
  def shortExc(msg: String) = throw new Exception(msg, null, false, false) {}

  /** If before given `end` address, the result of `op`, otherwise `default` */
  def ifBefore[T](end: Addr)(op: => T, default: T): T =
    if (bp < index(end)) op else default

  /** Perform `op` while condition `cond` holds and collect results in a list. */
  def collectWhile[T](cond: => Boolean)(op: => T): List[T] = {
    val buf = new ListBuffer[T]
    while (cond) buf += op
    buf.toList
  }
}
