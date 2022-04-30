package com.typesafe.tools.mima.core

import PickleFormat._

object MimaUnpickler {
  def unpickleClass(buf: PickleBuffer, clazz: ClassInfo, path: String): Unit = {
    if (buf.bytes.length == 0) return

    val doPrint = false
    // val doPrint = path.contains("v1") && !path.contains("exclude.class")
    if (doPrint) {
      println(s"unpickling $path")
      PicklePrinter.printPickle(buf)
    }

    buf.readNat(); buf.readNat() // major, minor version

    val index    = buf.createIndex
    val entries  = new Array[Entry](index.length)
    val classes  = new scala.collection.mutable.HashMap[SymInfo, ClassInfo]
    def syms     = entries.iterator.collect { case s: SymbolInfo => s }
    def defnSyms = syms.filter(sym => sym.tag == CLASSsym || sym.tag == MODULEsym)
    def methSyms = syms.filter(sym => sym.tag == VALsym)

    def until[T](end: Int, op: () => T): List[T] =
      if (buf.readIndex == end) Nil else op() :: until(end, op)

    def at[T <: Entry](num: Int, op: () => T): T = {
      var r = entries(num)
      if (r eq null) {
        buf.atIndex(index(num)) {
          r = op()
          assert(entries(num) == null, entries(num))
          r match {
            case _: UnknownType  =>
            case _: UnknownEntry =>
            case _               => entries(num) = r
          }
        }
      }
      r.asInstanceOf[T]
    }

    def readEnd(): Int              = buf.readNat() + buf.readIndex
    def readNameRef(): Name         = at(buf.readNat(), () => readName())
    def readSymRef(): SymInfo       = at(buf.readNat(), () => readSym(buf.readByte()))
    def readSymbolRef(): SymbolInfo = at(buf.readNat(), () => readSymbol(buf.readByte()))
    def readTypeRef(): TypeInfo     = at(buf.readNat(), () => readType())

    def readName(): Name = {
      val tag   = buf.readByte()
      val end   = readEnd()
      val bytes = buf.bytes.slice(buf.readIndex, end)
      tag match {
        case TERMname => TermName(new String(bytes, "UTF-8"))
        case TYPEname => TypeName(new String(bytes, "UTF-8"))
        case tag      => TypeName(s"?(tag=$tag)")
      }
    }

    def readSym(tag: Int): SymInfo = tag match {
      case NONEsym        => readSymbol(tag)
      case TYPEsym        => readSymbol(tag)
      case ALIASsym       => readSymbol(tag)
      case CLASSsym       => readSymbol(tag) // CLASSsym len_Nat SymbolInfo [thistype_Ref]
      case MODULEsym      => readSymbol(tag)
      case VALsym         => readSymbol(tag)
      case EXTref         => readExt(tag)
      case EXTMODCLASSref => readExt(tag)
      case tag            => sys.error(s"Unexpected tag ${tag2string(tag)}")
    }

    def readSymbol(tag: Int): SymbolInfo = {
      // SymbolInfo = name_Ref owner_Ref flags_LongNat [privateWithin_Ref] info_Ref
      if (tag == NONEsym) {
        buf.readIndex = readEnd()
        return NoSymbol
      }
      val end   = readEnd()
      val name  = readNameRef()
      val owner = readSymRef()
      val flags = buf.readLongNat()
      val (privateWithin, info) = buf.readNat() match {
        case info if buf.readIndex == end => (-1, info)
        case privateWithin                => (privateWithin, buf.readNat())
      }
      if (tag == CLASSsym && buf.readIndex != end) buf.readNat() // thistype_Ref
      buf.assertEnd(end)
      val isScopedPrivate = privateWithin != -1
      SymbolInfo(tag, name, owner, flags, isScopedPrivate)
    }

    def readExt(tag: Int): ExtInfo = {
      // EXTref         len_Nat name_Ref [owner_Ref]
      // EXTMODCLASSref len_Nat name_Ref [owner_Ref]
      val end   = readEnd()
      val name  = readNameRef()
      val owner = if (buf.readIndex == end) NoSymbol else readSymRef()
      buf.assertEnd(end)
      ExtInfo(tag, name, owner)
    }

    def readType(): TypeInfo = {
      //    THIStpe len_Nat sym_Ref
      // TYPEREFtpe len_Nat type_Ref sym_Ref {targ_Ref}
      val tag = buf.readByte()
      val end = readEnd()
      tag match {
        case THIStpe    => ThisTypeInfo(readSymRef())
        case TYPEREFtpe => TypeRefInfo(readTypeRef(), readSymRef(), until(end, () => readTypeRef()))
        case _          => UnknownType(tag)
      }
    }

    def readSymbolAnnotation(): SymAnnotInfo = {
      // SYMANNOT      = len_Nat sym_Ref AnnotInfoBody
      // AnnotInfoBody = info_Ref {annotArg_Ref} {name_Ref constAnnotArg_Ref}
      val end = readEnd()
      val sym = readSymbolRef()
      val tpe = readTypeRef()
      buf.readIndex = end
      SymAnnotInfo(sym, tpe)
    }

    def readEntry() = {
      val tag = buf.readByte()
      tag match {
        case NONEsym   => readSymbol(tag)
        case TYPEsym   => readSymbol(tag)
        case ALIASsym  => readSymbol(tag)
        case CLASSsym  => readSymbol(tag)
        case MODULEsym => readSymbol(tag)
        case VALsym    => readSymbol(tag)
        case SYMANNOT  => readSymbolAnnotation()
        case _         => buf.readIndex = readEnd(); UnknownEntry(tag)
      }
    }

    for (num <- index.indices) at(num, () => readEntry())

    if (doPrint) {
      entries.iterator.zipWithIndex.filter(_._1 != null).foreach { case (entry, num) =>
        println(s"$num: ${entry.getClass.getSimpleName} $entry")
      }
    }

    def symbolToClass(symbolInfo: SymbolInfo): ClassInfo =
      if (symbolInfo.name.value == REFINE_CLASS_NAME) {
        // eg: CLASSsym 4: 89(<refinement>) 0 0[] 87
        // Nsc's UnPickler also excludes these with "isRefinementSymbolEntry"
        NoClass
      } else if (symbolInfo.name.value == LOCAL_CHILD) {
        // Predef$$less$colon$less$<local child>
        NoClass
      } else {
        val fallback = if (symbolInfo.isModuleOrModuleClass) clazz.moduleClass else clazz
        def lookup(cls: ClassInfo) = {
          val clsName   = cls.bytecodeName
          val separator = if (clsName.endsWith("$")) "" else "$"
          val name0     = symbolInfo.name.value
          val name      = if (name0.startsWith(clsName)) name0.substring(name0.lastIndexOf('$') + 1) else name0
          val suffix    = if (symbolInfo.isModuleOrModuleClass) "$" else ""
          val newName   = clsName + separator + name + suffix
          clazz.owner.classes.getOrElse(newName, NoClass)
        }
        classes.getOrElse(symbolInfo.owner, null) match {
          case null if symbolInfo.owner == entries(0) => lookup(fallback)
          case null                                   => fallback
          case cls                                    => lookup(cls)
        }
      }

    def doMethods(clazz: ClassInfo, methods: List[SymbolInfo]) =
      methods.iterator
        .filter(!_.isParam)
        .filter(_.name.value != CONSTRUCTOR) // TODO support package private constructors
        .toSeq
        .groupBy(_.name)
        .foreach { case (name, pickleMethods) =>
          doMethodOverloads(clazz, name, pickleMethods)
        }

    def doMethodOverloads(clazz: ClassInfo, name: Name, pickleMethods: Seq[SymbolInfo]) = {
      val bytecodeMethods = clazz.methods.get(name.value).filter(!_.isBridge).toList
      // #630 one way this happens with mixins:
      //    trait Foo { def bar(x: Int): Int = x }
      //    class Bar extends Foo { private[foo] def bar: String = "" }
      // during pickling Bar only contains the package private bar()String
      // but later in the backend the classfile gets a copy of bar(Int)Int
      // so the "bar" method in the pickle doesn't know which bytecode method it's about
      // the proper way to fix this involves unpickling the types in the pickle,
      // then implementing the rules of erasure, so that you can then match the pickle
      // types with the erased types.  Meanwhile we'll just ignore them, worst case users
      // need to add a filter like they have for years.
      if (pickleMethods.size == bytecodeMethods.size && pickleMethods.exists(_.isScopedPrivate)) {
        bytecodeMethods.zip(pickleMethods).foreach { case (bytecodeMeth, pickleMeth) =>
          bytecodeMeth.scopedPrivate = pickleMeth.isScopedPrivate
        }
      }
    }

    for (sym <- defnSyms) classes(sym) = symbolToClass(sym)

    for (clsSym <- defnSyms) {
      val cls = classes(clsSym)
      if (clsSym.isScopedPrivate && cls != NoClass) cls.module._scopedPrivate = true
      doMethods(cls, methSyms.filter(_.owner == clsSym).toList)
    }

    for (symAnnot <- entries.iterator.collect { case s: SymAnnotInfo => s }) {
      val cls = classes.getOrElse(symAnnot.sym, null) // add support for @experimental methods?
      if (cls != null && cls != NoClass) {
        val annotName = symAnnot.tpe match {
          case ThisTypeInfo(sym)        => s"$sym"
          case TypeRefInfo(_, sym, Nil) => s"$sym"
          case _                        => "?"
        }
        cls._annotations :+= AnnotInfo(annotName)
      }
    }
  }

  sealed trait Entry {
    override def toString = this match {
      case TermName(value)              => s"$value."
      case TypeName(value)              => s"$value#"
      case x: SymInfo                   => s"$x"
      case ThisTypeInfo(sym)            => s"$sym"
      case TypeRefInfo(tpe, sym, targs) => s"$sym${if (targs.isEmpty) "" else targs.mkString("[", ", ", "]")}"
      case SymAnnotInfo(sym, tpe)       => s"@$tpe $sym"
      case UnknownType(tag)             => s"UnknownType(${tag2string(tag)})"
      case UnknownEntry(tag)            => s"UnknownEntry(${tag2string(tag)})"
    }
  }
  final case class UnknownEntry(tag: Int) extends Entry

  sealed trait Name extends Entry { def tag: Int; def value: String }
  final case class TermName(value: String) extends Name { def tag = TERMname }
  final case class TypeName(value: String) extends Name { def tag = TYPEname }

  object nme {
    def NoSymbol = TermName("NoSymbol")
    def Empty    = TermName("<empty>")
  }

  sealed trait SymInfo extends Entry {
    def tag: Int
    def name: Name
    def owner: SymInfo

    def isNoSymbol = tag == NONEsym && name == nme.NoSymbol
    def isEmpty    = name == nme.Empty

    override def toString =
      if (isNoSymbol) "NoSymbol"
      else if (owner.isNoSymbol || owner.isEmpty) name.value
      else s"$owner.${name.value}"
  }

  final case class SymbolInfo(tag: Int, name: Name, owner: SymInfo, flags: Long, isScopedPrivate: Boolean)
      extends SymInfo {
    def hasFlag(flag: Long): Boolean = (flags & flag) != 0L
    def isModuleOrModuleClass        = hasFlag(Flags.MODULE_PKL)
    def isParam                      = hasFlag(Flags.PARAM)
  }
  val NoSymbol: SymbolInfo = SymbolInfo(NONEsym, nme.NoSymbol, null, 0, false)

  final case class ExtInfo(tag: Int, name: Name, owner: SymInfo) extends SymInfo

  sealed trait TypeInfo extends Entry
  final case class UnknownType(tag: Int) extends TypeInfo

  final case class ThisTypeInfo(sym: SymInfo) extends TypeInfo
  final case class TypeRefInfo(tpe: TypeInfo, sym: SymInfo, targs: List[TypeInfo]) extends TypeInfo

  final case class SymAnnotInfo(sym: SymbolInfo, tpe: TypeInfo) extends Entry

  def readNat(data: Array[Byte]): Int = {
    var idx = 0
    var res = 0L
    var b   = 0L
    while ({
      b = data(idx).toLong
      idx += 1
      res = (res << 7) + (b & 0x7f)
      (b & 0x80) != 0L
    }) ()
    res.toInt
  }

  object Flags {
    final val MODULE_PKL = 1L << 10
    final val PARAM      = 1L << 13
  }

  final val CONSTRUCTOR       = "<init>"
  final val LOCAL_CHILD       = "<local child>"
  final val REFINE_CLASS_NAME = "<refinement>"
}
