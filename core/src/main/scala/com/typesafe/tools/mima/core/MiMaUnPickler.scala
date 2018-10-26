package com.typesafe.tools.mima.core

import java.io.IOException

import scala.io.Codec

import scala.reflect.internal._
import scala.reflect.internal.Flags._
import scala.reflect.internal.pickling._
import scala.reflect.internal.pickling.PickleFormat._

final class MiMaUnPickler {
  /** Unpickle symbol table information descending from a class and/or module root
   *  from an array of bytes.
   *  @param bytes      bytearray from which we unpickle
   *  @param offset     offset from which unpickling starts
   *  @param classRoot  the top-level class which is unpickled, or NoSymbol if inapplicable
   */
  def unpickle(bytes: Array[Byte], offset: Int, classRoot: ClassInfo) {
    try {
      new Scan(bytes, offset, classRoot).run()
    } catch {
      case ex: IOException =>
        throw ex
      case ex: MissingRequirementError =>
        throw ex
      case ex: Throwable =>
        ex.printStackTrace()
        throw new RuntimeException("error reading Scala signature: " + ex.getMessage)
    }
  }

  final class Scan(_bytes: Array[Byte], offset: Int, classRoot: ClassInfo)
      extends PickleBuffer(_bytes, offset, -1)
  {

    {
      val major = readNat()
      val minor = readNat()
    }

    /** A map from entry numbers to array offsets */
    private val index = createIndex

    /** A map from entry numbers to symbols, types, or annotations */
    private val entries = new Array[AnyRef](index.length)

    // Laboriously unrolled for performance.
    def run() {
      val i = 0
      if (index.length == 0) return

      if (entries(i) == null) {
        if (isSymbolEntry(i)) {
          val savedIndex = readIndex
          readIndex = index(i)
          val symbol = readSymbol()
          entries(i) = symbol
          classRoot.privateWithin = symbol.privateWithin.name
          readIndex = savedIndex
        }
      }
    }

    /** Read an annotation and as a side effect store it into
     *  the symbol it requests. Called at top-level, for all
     *  (symbol, annotInfo) entries. */
    protected def readSymbolAnnotation() {
      val tag = readByte()
      if (tag != SYMANNOT)
        ???
      val end = readNat() + readIndex
      val target = readSymbolRef()
      val value = readAnnotationInfo(end)
    }

    /** Read an AnnotationInfo. Not to be called directly, use
     *  readAnnotation or readSymbolAnnotation
     */
    protected def readAnnotationInfo(end: Int) = {
      while (readIndex != end) {
        readNat()
      }
    }

    /** Does entry represent a symbol annotation? */
    protected def isSymbolAnnotationEntry(i: Int): Boolean = {
      val tag = bytes(index(i)).toInt
      tag == SYMANNOT
    }

    /** Does the entry represent children of a symbol? */
    protected def isChildrenEntry(i: Int): Boolean = {
      val tag = bytes(index(i)).toInt
      tag == CHILDREN
    }

    /** Does entry represent an (internal) symbol */
    protected def isSymbolEntry(i: Int): Boolean = {
      val tag = bytes(index(i)).toInt
      (firstSymTag <= tag && tag <= lastSymTag &&
       (tag != CLASSsym || !isRefinementSymbolEntry(i)))
    }

    /** Does entry represent a refinement symbol?
     *  pre: Entry is a class symbol
     */
    protected def isRefinementSymbolEntry(i: Int): Boolean = {
      val savedIndex = readIndex
      readIndex = index(i)
      val tag = readByte()
      assert(tag == CLASSsym)

      readNat(); // read length
      val result = readNameRef() == "<refinement>"
      readIndex = savedIndex
      result
    }

    /** Does entry represent an (internal or external) symbol */
    protected def isSymbolRef(i: Int): Boolean = {
      val tag = bytes(index(i))
      (firstSymTag <= tag && tag <= lastExtSymTag)
    }

    /** If entry at <code>i</code> is undefined, define it by performing
     *  operation <code>op</code> with <code>readIndex at start of i'th
     *  entry. Restore <code>readIndex</code> afterwards.
     */
    protected def at[T <: AnyRef](i: Int, op: () => T): T = {
      var r = entries(i)
      if (r eq null) {
        val savedIndex = readIndex
        readIndex = index(i)
        r = op()
        assert(entries(i) eq null, entries(i))
        entries(i) = r
        readIndex = savedIndex
      }
      r.asInstanceOf[T]
    }

    type Name = String

    final case class Symbol(name: String, privateWithin: Symbol)
    lazy val NoSymbol: Symbol = Symbol("", null)

    /** Read a name */
    protected def readName(): Name = {
      val tag = readByte()
      val len = readNat()
      tag match {
        case TERMname => new String(Codec.fromUTF8(bytes, readIndex, len))
        case TYPEname => new String(Codec.fromUTF8(bytes, readIndex, len))
        case _ => ???
      }
    }
    /** Read a symbol */
    protected def readSymbol(): Symbol = {
      val tag   = readByte()
      val end   = readNat() + readIndex
      def atEnd = readIndex == end

      tag match {
        case NONEsym                 => return NoSymbol
        case EXTref | EXTMODCLASSref => return Symbol(readNameRef(), NoSymbol)
        case _                       => ()
      }

      // symbols that were pickled with Pickler.writeSymInfo
      val nameref      = readNat()
      val name         = at(nameref, readName)
      val owner        = readSymbolRef()
      val flags        = pickledToRawFlags(readLongNat())
      var inforef      = readNat()
      val privateWithin =
        if (!isSymbolRef(inforef)) NoSymbol
        else {
          val pw = at(inforef, readSymbol)
          inforef = readNat()
          pw
        }

      while (!atEnd) readNat()

      Symbol(name, privateWithin)
    }

    /* Read a reference to a pickled item */
    protected def readSymbolRef(): Symbol             = at(readNat(), readSymbol)
    protected def readNameRef(): Name                 = at(readNat(), readName)
  }
}
