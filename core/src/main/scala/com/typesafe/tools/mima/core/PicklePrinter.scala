package com.typesafe.tools.mima.core

import java.lang.Long.toHexString
import java.lang.Float.intBitsToFloat
import java.lang.Double.longBitsToDouble
import scala.annotation.tailrec
import scala.io.Codec

import PickleFormat._

object PicklePrinter {
  def printPickle(buf: PickleBuffer): Unit = buf.atIndex(0) {
    println(s"Version ${buf.readNat()}.${buf.readNat()}")

    val index = buf.createIndex
    val entries = PickleEntries(toIndexedSeq(buf).zipWithIndex.map { case ((tag, data), num) =>
      PickleEntry(num, index(num), tag, data)
    })
    buf.readIndex = 0

    def p(s: String)            = print(s)
    def nameAt(idx: Int)        = s"$idx(${entries.nameAt(idx)})"
    def printNat()              = p(s" ${buf.readNat()}")
    def printNameRef()          = p(s" ${nameAt(buf.readNat())}")
    def printSymbolRef()        = printNat()
    def printTypeRef()          = printNat()
    def printConstantRef()      = printNat()
    def printAnnotInfoRef()     = printNat()
    def printConstAnnotArgRef() = printNat()
    def printAnnotArgRef()      = printNat()

    def printSymInfo(end: Int): Unit = {
      printNameRef()
      printSymbolRef() // owner
      val pflags = buf.readLongNat()
      val (flagString, info) = buf.readNat() match {
        case pw if buf.readIndex != end => (nameAt(pw), buf.readNat())
        case info                       => ("", info)
      }
      p(s" ${toHexString(pflags)}[$flagString] $info")
    }

    def printEntry(i: Int): Unit = {
      buf.readIndex = index(i)
      p(s"$i,${buf.readIndex}: ")

      val tag = buf.readByte()
      p(tag2string(tag))

      val len = buf.readNat()
      val end = len + buf.readIndex
      p(s" $len:")

      def all[T](body: => T): List[T] = if (buf.readIndex == end) Nil else body :: all(body)
      def printTypes()                = all(printTypeRef())
      def printSymbols()              = all(printSymbolRef())

      @tailrec def printTag(tag: Int): Unit = tag match {
        case TERMname => p(s" ${showName(buf.bytes, buf.readIndex, len)}"); buf.readIndex = end
        case TYPEname => p(s" ${showName(buf.bytes, buf.readIndex, len)}"); buf.readIndex = end

        case NONEsym   =>
        case TYPEsym   => printSymInfo(end)
        case ALIASsym  => printSymInfo(end)
        case CLASSsym  => printSymInfo(end); if (buf.readIndex < end) printTypeRef()
        case MODULEsym => printSymInfo(end)
        case VALsym    => printSymInfo(end)

        case EXTref         => printNameRef(); if (buf.readIndex < end) printSymbolRef()
        case EXTMODCLASSref => printNameRef(); if (buf.readIndex < end) printSymbolRef()

        case NOtpe             =>
        case NOPREFIXtpe       =>
        case THIStpe           => printSymbolRef()
        case SINGLEtpe         => printTypeRef(); printSymbolRef()
        case CONSTANTtpe       => printTypeRef(); printConstantRef()
        case TYPEREFtpe        => printTypeRef(); printSymbolRef(); printTypes()
        case TYPEBOUNDStpe     => printTypeRef(); printTypeRef()
        case REFINEDtpe        => printSymbolRef(); printTypes()
        case CLASSINFOtpe      => printSymbolRef(); printTypes()
        case METHODtpe         => printTypeRef(); printTypes()
        case POLYtpe           => printTypeRef(); printSymbols()
        case IMPLICITMETHODtpe => printTypeRef(); printTypes()
        case SUPERtpe          => printTypeRef(); printTypeRef()

        case LITERALunit    =>
        case LITERALboolean => p(if (buf.readLong(len) == 0L) " false" else " true")
        case LITERALbyte    => p(" " + buf.readLong(len).toByte)
        case LITERALshort   => p(" " + buf.readLong(len).toShort)
        case LITERALchar    => p(" " + buf.readLong(len).toChar)
        case LITERALint     => p(" " + buf.readLong(len).toInt)
        case LITERALlong    => p(" " + buf.readLong(len))
        case LITERALfloat   => p(" " + intBitsToFloat(buf.readLong(len).toInt))
        case LITERALdouble  => p(" " + longBitsToDouble(buf.readLong(len)))
        case LITERALstring  => printNameRef(); p(scala.io.AnsiColor.RESET)
        case LITERALnull    => p(" <null>")
        case LITERALclass   => printTypeRef()
        case LITERALenum    => printSymbolRef()
        case LITERALsymbol  => printNameRef()

        case SYMANNOT         => printSymbolRef(); printTag(ANNOTINFO)
        case CHILDREN         => printSymbolRef(); printSymbols()
        case ANNOTATEDtpe     => printTypeRef(); all(printAnnotInfoRef())
        case ANNOTINFO        => printTypeRef(); all(printAnnotArgRef())
        case ANNOTARGARRAY    => all(printConstAnnotArgRef())
        case DEBRUIJNINDEXtpe => printNat(); printNat()
        case EXISTENTIALtpe   => printTypeRef(); printSymbols()

        case TREE      => // skipped
        case MODIFIERS => // skipped
        case _ => throw new RuntimeException(s"malformed Scala signature at ${buf.readIndex}; unknown tree type ($tag)")
      }
      printTag(tag)

      println()
      if (buf.readIndex != end) {
        val bytes = buf.bytes.slice(index(i), end.max(buf.readIndex)).mkString(", ")
        println(s"BAD ENTRY END: computed = $end, actual = ${buf.readIndex}, bytes = $bytes")
      }
    }

    for (i <- index.indices)
      printEntry(i)
  }

  private def showName(bs: Array[Byte], idx: Int, len: Int) = new String(Codec.fromUTF8(bs, idx, len))

  final private case class PickleEntry(num: Int, startIndex: Int, tag: Int, bytes: Array[Byte]) {
    override def toString = s"$num,$startIndex: ${tag2string(tag)}"
  }

  final private case class PickleEntries(entries: IndexedSeq[PickleEntry]) {
    def nameAt(idx: Int) = entries(idx) match {
      case PickleEntry(_, _, TERMname, bytes) => new String(bytes, "UTF-8")
      case PickleEntry(_, _, TYPEname, bytes) => new String(bytes, "UTF-8")
      case _                                  => "?"
    }
  }

  /**
   * Returns the buffer as a sequence of (Int, Array[Byte]) representing (tag, data) of the individual entries. Saves
   * and restores buffer state.
   */
  private def toIndexedSeq(buf: PickleBuffer): IndexedSeq[(Int, Array[Byte])] = (
    for (idx <- buf.createIndex) yield buf.atIndex(idx) {
      val tag = buf.readByte()
      val len = buf.readNat()
      tag -> buf.bytes.slice(buf.readIndex, buf.readIndex + len)
    }
  ).toIndexedSeq
}
