package ssol.tools.mima.client

import java.io._
import scala.tools.nsc.io.{ AbstractFile, PlainFile }
import scala.tools.nsc.symtab.classfile.ClassfileConstants._
import scala.collection.mutable.ArrayBuffer

import ssol.tools.mima.core._
import ssol.tools.mima.core.Config._

object Fix {
  import scala.collection.mutable.ListBuffer

  /** Return a list of fixes for the given problems. */
  def libFixesFor(problems: List[IncompatibleResultTypeProblem]): List[Fix] = {
    val fixes = new ListBuffer[Fix]

    
    for ((clazz, problems) <- problems groupBy (_.newmeth.owner)) {
      fixes += new LibFix(clazz)(problems map (p => (p.newmeth, p.oldmeth.sig))).fix()
    }

    fixes.toList
  }

}

abstract class Fix(val clazz: ClassInfo) {
  import ssol.tools.mima.core.util.log.ConsoleLogging._
  
  private final val Icat = 0
  private final val Lcat = 1
  private final val Fcat = 2
  private final val Dcat = 3
  private final val Acat = 4
  private final val Vcat = 5

  /** Returns the file with the given suffix for the given class. */
  private def outputFile(clazz: ClassInfo): File = {
    var outdir = Config.outDir.jfile
    val filename = clazz.fullName
    var start = 0
    var end = filename.indexOf('.', start)
    while (end >= start) {
      outdir = new File(outdir, filename.substring(start, end))
      if (!outdir.exists()) outdir.mkdir()
      start = end + 1
      end = filename.indexOf('.', start)
    }
    new File(outdir, filename.substring(start) + ".class")
  }

  val (outputStream: OutputStream, outputFileName: String) =
    if (Config.inPlace)
      (new ByteArrayOutputStream(), clazz.file.toString)
    else {
      val outFile = outputFile(clazz)
      (new BufferedOutputStream(new FileOutputStream(outFile)), outFile.toString)
    }

  protected val trans = new ClassfileTransformer(new DataOutputStream(outputStream), clazz.owner.definitions)
  import trans._

  parse(clazz)

  def fix(): this.type

  private def categories(sig: String): (List[Int], Int) = {
    def category(tag: Char) = tag match {
      case BYTE_TAG | CHAR_TAG | SHORT_TAG | INT_TAG | BOOL_TAG => Icat
      case LONG_TAG => Lcat
      case FLOAT_TAG => Fcat
      case DOUBLE_TAG => Dcat
      case VOID_TAG => Vcat
      case 'L' => Acat
      case '[' => Acat
    }
    def loop(i: Int): (List[Int], Int) = sig(i) match {
      case ')' =>
        (List(), category(sig(i + 1)))
      case 'L' =>
        val (cs, c) = loop(sig.indexOf(';', i + 1) + 1)
        (Acat :: cs, c)
      case '[' =>
        val (cs, c) = loop(i + 1)
        (Acat :: cs.tail, c)
      case tag =>
        val (cs, c) = loop(i + 1)
        (category(tag) :: cs, c)
    }
    loop(1)
  }

  private def numWords(cat: Int) = cat match {
    case Icat | Fcat | Acat => 1
    case Lcat | Dcat        => 2
    case Vcat               => 0
  }

  private def load(category: Int, offset: Int) =
    if (offset <= 3) (iload_0 + category * 4 + offset).toByte
    else if (offset <= 255) Code(iload.toByte, offset.toByte)
    else Code(wide.toByte, iload.toByte, offset.toChar)

  private def loadArgs(argcats: List[Int], firstOffset: Int): Code =
    Code((for ((ac, idx) <- argcats.zipWithIndex) yield load(ac, idx + firstOffset)): _*)

  protected def addField(getter: MemberInfo): Field = {
    debugLog("add field for " + getter + ":" + getter.sig)
    new Field(
      namestr = getter.name,
      flags = getter.flags & JAVA_ACC_FINAL,
      sigstr = getter.sig.dropWhile(_ != ')').tail
    )
  }

  protected def addGetter(missing: MemberInfo, fld: Field): Method = {
    val (List(), rescat) = categories(missing.sig)
    debugLog("add getter for " + missing + ":" + missing.sig + ": " + rescat)
    val targetRef = new FieldRef(clazz, fld.namestr, fld.sigstr)
    val code = Code(
      aload_0.toByte,
      getfield.toByte, pool.index(targetRef).toChar,
      (ireturn + rescat).toByte)
    new Method(missing, numWords(rescat), 1, code)
  }

  protected def addSetter(missing: MemberInfo, fld: Field): Method = {
    val (List(argcat), Vcat) = categories(missing.sig)
    debugLog("add setter for " + missing + ":" + missing.sig + ": " + argcat)
    val targetRef = new FieldRef(clazz, fld.namestr, fld.sigstr)
    val code = Code(
      aload_0.toByte,
      load(argcat, 1),
      putfield.toByte, pool.index(targetRef).toChar,
      return_.toByte)
    val stackSize = 1 + numWords(argcat)
    new Method(missing, stackSize, stackSize, code)
  }

  protected def addForwarder(missing: MemberInfo): Method = {
    val (argcats, rescat) = categories(missing.sig)
    debugLog("add forwarder method for " + missing + ":" + missing.sig + ": " + argcats + "/" + rescat)
    val targetMeth = missing.staticImpl.get
    val targetRef = new MethodRef(targetMeth)
    val code = Code(
      aload_0.toByte,
      loadArgs(argcats, 1),
      invokestatic.toByte, pool.index(targetRef).toChar,
      (ireturn + rescat).toByte)
    val stackSize = (argcats map numWords).sum + (1 max numWords(rescat))
    new Method(missing, stackSize, stackSize, code)
  }

  protected def addBridge(existing: MemberInfo, sig: String): Method = {
    val (argcats, rescat) = categories(sig)
    debugLog("add bridge method for " + existing + ":" + sig)
    val bridge = new MemberInfo(clazz, existing.name, existing.flags & ~JAVA_ACC_ABSTRACT, sig)
    val targetRef = new MethodRef(existing)
    val (code, stackSize) =
      if ((existing.flags & JAVA_ACC_STATIC) != 0)
        (Code(
          loadArgs(argcats, 0),
          invokestatic.toByte, pool.index(targetRef).toChar,
          (ireturn + rescat).toByte),
          (argcats map numWords).sum + numWords(rescat))
      else
        (Code(
          aload_0.toByte,
          loadArgs(argcats, 1),
          invokevirtual.toByte, pool.index(targetRef).toChar,
          (ireturn + rescat).toByte),
          (argcats map numWords).sum + (1 max numWords(rescat)))
    new Method(bridge, stackSize, stackSize, code)
  }

  private def target(instr: Instruction) = instr.target(trans)

  /** A list of all instructions where this constructor calls another
   *  constructor of a class or implementation class
   */
  private def constrCalls(constructor: MemberInfo): List[Instruction] = {
    val (start, end) = constructor.codeOpt.get
    val instrs = new InstructionIterator(start, end)

    def superSelfCall(): Instruction = {
      var outstandingNews = 0
      while (instrs.hasNext) {
        val i = instrs.next
        if (i.instr == new_) outstandingNews += 1
        else if (i.instr == invokespecial && target(i).name == "<init>") {
          outstandingNews -= 1
          if (outstandingNews < 0) return i
        }
      }
      throw new AssertionError("no super or self call found in " + constructor + " of " + clazz)
    }

    def implClassInits(): List[Instruction] =
      instrs.filter { i =>
        i.instr == invokestatic &&
          target(i).clazz.isImplClass &&
          target(i).name == implClassInitName
      }.toList

    superSelfCall :: implClassInits
  }

  private def isPrimary(supercalls: List[Instruction]) =
    target(supercalls.head).clazz == clazz.superClass

  private def implClassInitName = "$init$"

  private def typeSigOfClass(clazz: ClassInfo): String = "L" + external(clazz.fullName) + ";"

  private def initPatch(iface: ClassInfo, offset: Int): Patch = {
    debugLog("add init call to " + iface.implClass)
    val targetRef = new MethodRef(
      iface.implClass, implClassInitName, "(" + typeSigOfClass(iface) + ")V")
    val code = Code(
      aload_0.toByte,
      invokestatic.toByte, pool.index(targetRef).toChar)
    new Patch(offset, code.write)
  }

  private def fixInits(inherited: List[ClassInfo], initCalls: List[Instruction]): List[Patch] = {
    def corresponds(t: ClassInfo, call: Instruction) =
      t.implClass == target(call).clazz
    (inherited, initCalls) match {
      case (List(), _) =>
        List()
      case (t :: ts, prev :: call :: calls) if corresponds(t, call) =>
        fixInits(ts, call :: calls)
      case (t :: ts, prev :: calls) =>
        assert(calls forall (c => !corresponds(t, c)))
        initPatch(t, prev.offset + 3) :: fixInits(ts, initCalls)
    }
  }

  private def fixInits(constr: MemberInfo): List[Patch] = {
    val ccalls = constrCalls(constr)
    if (isPrimary(ccalls)) {
      val newInits = fixInits(clazz.directTraits, ccalls)
      val incr = newInits.length * 4
      if (incr > 0) {
        val start = constr.codeOpt.get._1
        incIntCountPatch(start - 12, incr) :: // attr_length
          incIntCountPatch(start - 4, incr) :: // code_length
          newInits
      } else List()
    } else List()
  }

  protected def fixInits: List[Patch] = clazz.constructors flatMap fixInits
}

class LibFix(clazz: ClassInfo)(gaps: List[(MemberInfo, String)]) extends Fix(clazz) {
  override def fix(): this.type = {
    val methodFixups = for ((target, sig) <- gaps) yield addBridge(target, sig)
    trans.writeClassFile(List(), methodFixups, List())
    this
  }
}

class ClientFix(clazz: ClassInfo) extends Fix(clazz) {
  override def fix(): this.type = {
    val (fieldFixups, accessorFixups) = {
      for (setter <- clazz.unimplementedSetters) yield {
        val getter = setter.getter
        val fld = addField(getter)
        (fld, List(addGetter(getter, fld), addSetter(setter, fld)))
      }
    }.unzip
    val methodFixups = clazz.unimplementedMethods map addForwarder
    trans.writeClassFile(fieldFixups, accessorFixups.flatten ++ methodFixups, fixInits)
    this
  }
}