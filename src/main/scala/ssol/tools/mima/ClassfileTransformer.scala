/* NSC -- new Scala compiler
 * Copyright 2005-2010 LAMP/EPFL
 * @author  Martin Odersky
 */

package ssol.tools.mima

import java.io._
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.symtab.classfile.{ClassfileConstants => JVM}
import JVM._
import scala.collection.mutable

/** This abstract class implements a class file transformer.
 *
 *  @author Martin Odersky
 *  @version 1.0
 */
class ClassfileTransformer(out: DataOutputStream) extends ClassfileParser {

  def readFields = (clazz: ClassInfo) => false
  def readMethods = (clazz: ClassInfo) => true
  def readCode = (meth: MemberInfo) => false

  private var thepool: ExtensibleConstantPool = _
  override def pool: ExtensibleConstantPool = thepool

  private var poolStart, poolEnd, fieldsStart, methodsStart, methodsEnd: Int = _

  var constrCodes = Map[MemberInfo, Iterator[Instruction]]()

  override def parseAll(clazz: ClassInfo) {
    parseHeader()
    poolStart = in.bp
    thepool = new ExtensibleConstantPool
    poolEnd = in.bp
    parseClass(clazz)
  }

  def external(s: String): String = s.replace('.', '/')

  class ExtensibleConstantPool extends ConstantPool { 
    
    val index = new mutable.LinkedHashMap[Entry, Int]
    
    def add(entry: Entry): Unit = index get entry match {
      case None => index += (entry -> (length + index.size))
      case _ =>
    }

    def writeNew() = index.keys foreach { entry =>
      Config.debugLog("CP"+index(entry)+": "+entry)
      entry.write()
    }

    lazy val codeTag = index(Name("Code"))
  }

  abstract class Entry {
    pool.add(this)
    def write()
  }

  case class Name(str: String) extends Entry {
    def write() {
      out.writeByte(CONSTANT_UTF8)
      out.writeUTF(str)
    }
  }

  case class ClassRef(name: Name) extends Entry {
    def this(clazz: ClassInfo) = this(Name(external(clazz.fullName)))
    def write() {
      out.writeByte(CONSTANT_CLASS)
      out.writeChar(pool.index(name))
    }
  }

  case class NameAndType(name: Name, tpe: Name) extends Entry {
    def this(name: String, tpe: String) = this(Name(name), Name(tpe))
    def write() {
      out.writeByte(CONSTANT_NAMEANDTYPE)
      out.writeChar(pool.index(name))
      out.writeChar(pool.index(tpe))
    }
  }

  case class FieldRef(clazz: ClassRef, nametpe: NameAndType) extends Entry {
    def this(owner: ClassInfo, name: String, tpe: String) = this(new ClassRef(owner), new NameAndType(name, external(tpe)))
    def write() {
      out.writeByte(CONSTANT_FIELDREF)
      out.writeChar(pool.index(clazz))
      out.writeChar(pool.index(nametpe))
    }
  }

  case class MethodRef(clazz: ClassRef, nametpe: NameAndType) extends Entry {
    def this(owner: ClassInfo, name: String, tpe: String) = this(new ClassRef(owner), new NameAndType(name, external(tpe)))
    def this(meth: MemberInfo) = this(meth.owner, meth.name, external(meth.sig))
    def write() {
      out.writeByte(CONSTANT_METHODREF)
      out.writeChar(pool.index(clazz))
      out.writeChar(pool.index(nametpe))
    }
  }

  override def parseFields(clazz: ClassInfo) {
    fieldsStart = in.bp
    skipMembers()
  }

  override def parseMethods(clazz: ClassInfo) {
    methodsStart = in.bp
    skipMembers()
    methodsEnd = in.bp
  }

  class Field(val namestr: String, val flags: Int, val sigstr: String) {
    val name = Name(namestr)
    val sig = Name(external(sigstr))
    
    def write() {
      out.writeChar(flags)
      out.writeChar(pool.index(name))
      out.writeChar(pool.index(sig))
      out.writeChar(0) // attribute count
    }
  }

  class InstructionIterator(start: Int, end: Int) extends Iterator[Instruction] {
    lazy val buf = ClassfileTransformer.this.in.buf.slice(start, end)
    lazy val in = new BufferReader(buf)

    def hasNext: Boolean = in.bp < buf.length

    def next(): Instruction = next(false)

    def next(wide: Boolean): Instruction = {
      def nextByte: Int = if (wide) in.nextChar else in.nextByte
      def nextChar: Int = if (wide) in.nextInt else in.nextChar
      def nextInt : Int = in.nextInt
      if (hasNext) {
        val offset = start + in.bp
        val instr = in.nextByte & 0xff
        var arg = 0
        instr match {
          case JVM.nop         => next()
          case JVM.aconst_null => 
          case JVM.iconst_m1   => 
          case JVM.iconst_0    => 
          case JVM.iconst_1    => 
          case JVM.iconst_2    => 
          case JVM.iconst_3    => 
          case JVM.iconst_4    => 
          case JVM.iconst_5    => 
          case JVM.lconst_0    => 
          case JVM.lconst_1    => 
          case JVM.fconst_0    => 
          case JVM.fconst_1    => 
          case JVM.fconst_2    => 
          case JVM.dconst_0    => 
          case JVM.dconst_1    => 

          case JVM.bipush      => arg = nextByte
          case JVM.sipush      => arg = nextChar
          case JVM.ldc         => arg = nextByte
          case JVM.ldc_w       => arg = nextChar
          case JVM.ldc2_w      => arg = nextChar
          case JVM.iload       => arg = nextByte
          case JVM.lload       => arg = nextByte
          case JVM.fload       => arg = nextByte
          case JVM.dload       => arg = nextByte
          case JVM.aload       => arg = nextByte
          case JVM.iload_0     => 
          case JVM.iload_1     => 
          case JVM.iload_2     => 
          case JVM.iload_3     => 
          case JVM.lload_0     => 
          case JVM.lload_1     => 
          case JVM.lload_2     => 
          case JVM.lload_3     => 
          case JVM.fload_0     => 
          case JVM.fload_1     => 
          case JVM.fload_2     => 
          case JVM.fload_3     => 
          case JVM.dload_0     => 
          case JVM.dload_1     => 
          case JVM.dload_2     => 
          case JVM.dload_3     => 
          case JVM.aload_0     =>
          case JVM.aload_1     => 
          case JVM.aload_2     => 
          case JVM.aload_3     => 

          case JVM.iaload      => 
          case JVM.laload      => 
          case JVM.faload      => 
          case JVM.daload      => 
          case JVM.aaload      => 
          case JVM.baload      => 
          case JVM.caload      => 
          case JVM.saload      => 

          case JVM.istore      => arg = nextByte
          case JVM.lstore      => arg = nextByte
          case JVM.fstore      => arg = nextByte
          case JVM.dstore      => arg = nextByte
          case JVM.astore      => arg = nextByte
          case JVM.istore_0    => 
          case JVM.istore_1    => 
          case JVM.istore_2    => 
          case JVM.istore_3    => 
          case JVM.lstore_0    => 
          case JVM.lstore_1    => 
          case JVM.lstore_2    => 
          case JVM.lstore_3    => 
          case JVM.fstore_0    => 
          case JVM.fstore_1    => 
          case JVM.fstore_2    => 
          case JVM.fstore_3    => 
          case JVM.dstore_0    => 
          case JVM.dstore_1    => 
          case JVM.dstore_2    => 
          case JVM.dstore_3    => 
          case JVM.astore_0    =>
          case JVM.astore_1    => 
          case JVM.astore_2    => 
          case JVM.astore_3    => 
          case JVM.iastore     => 
          case JVM.lastore     => 
          case JVM.fastore     => 
          case JVM.dastore     => 
          case JVM.aastore     => 
          case JVM.bastore     => 
          case JVM.castore     => 
          case JVM.sastore     => 

          case JVM.pop         => 
          case JVM.pop2        => 
          case JVM.dup         => 
          case JVM.dup_x1      => 
          case JVM.dup_x2      => 
          case JVM.dup2        => 
          case JVM.dup2_x1     => 
          case JVM.dup2_x2     => 
          case JVM.swap        => 

          case JVM.iadd        => 
          case JVM.ladd        => 
          case JVM.fadd        => 
          case JVM.dadd        => 
          case JVM.isub        => 
          case JVM.lsub        => 
          case JVM.fsub        => 
          case JVM.dsub        => 
          case JVM.imul        => 
          case JVM.lmul        => 
          case JVM.fmul        => 
          case JVM.dmul        => 
          case JVM.idiv        => 
          case JVM.ldiv        => 
          case JVM.fdiv        => 
          case JVM.ddiv        => 
          case JVM.irem        => 
          case JVM.lrem        => 
          case JVM.frem        => 
          case JVM.drem        => 

          case JVM.ineg        => 
          case JVM.lneg        => 
          case JVM.fneg        => 
          case JVM.dneg        => 

          case JVM.ishl        => 
          case JVM.lshl        => 
          case JVM.ishr        => 
          case JVM.lshr        => 
          case JVM.iushr       => 
          case JVM.lushr       => 
          case JVM.iand        => 
          case JVM.land        => 
          case JVM.ior         => 
          case JVM.lor         => 
          case JVM.ixor        => 
          case JVM.lxor        => 
          case JVM.iinc        => arg = nextByte; nextByte

          case JVM.i2l         => 
          case JVM.i2f         => 
          case JVM.i2d         => 
          case JVM.l2i         => 
          case JVM.l2f         => 
          case JVM.l2d         => 
          case JVM.f2i         => 
          case JVM.f2l         => 
          case JVM.f2d         => 
          case JVM.d2i         => 
          case JVM.d2l         => 
          case JVM.d2f         => 
          case JVM.i2b         => 
          case JVM.i2c         => 
          case JVM.i2s         => 

          case JVM.lcmp        => 
          case JVM.fcmpl       => 
          case JVM.fcmpg       => 
          case JVM.dcmpl       => 
          case JVM.dcmpg       => 

          case JVM.ifeq        => arg = nextChar
          case JVM.ifne        => arg = nextChar
          case JVM.iflt        => arg = nextChar
          case JVM.ifge        => arg = nextChar
          case JVM.ifgt        => arg = nextChar
          case JVM.ifle        => arg = nextChar

          case JVM.if_icmpeq   => arg = nextChar
          case JVM.if_icmpne   => arg = nextChar
          case JVM.if_icmplt   => arg = nextChar
          case JVM.if_icmpge   => arg = nextChar
          case JVM.if_icmpgt   => arg = nextChar
          case JVM.if_icmple   => arg = nextChar
          case JVM.if_acmpeq   => arg = nextChar
          case JVM.if_acmpne   => arg = nextChar

          case JVM.goto        => arg = nextChar
          case JVM.jsr         => arg = nextChar
          case JVM.ret         => arg = nextByte
          case JVM.tableswitch =>
            in.bp = (in.bp + 3) & ~3
            val default = nextInt
            val low = nextInt
            val high = nextInt
            for (i <- low to high) {
              nextInt
            }

          case JVM.lookupswitch =>
            in.bp = (in.bp + 3) & ~3
            val default = nextInt
            val npairs = nextInt
            for (i <- 0 until npairs) { 
              nextInt
              nextInt
            }

          case JVM.ireturn     => 
          case JVM.lreturn     => 
          case JVM.freturn     => 
          case JVM.dreturn     => 
          case JVM.areturn     => 
          case JVM.return_     => 

          case JVM.getstatic   => arg = nextChar
          case JVM.putstatic   => arg = nextChar
          case JVM.getfield    => arg = nextChar
          case JVM.putfield    => arg = nextChar

          case JVM.invokevirtual   => arg = nextChar
          case JVM.invokeinterface => arg = nextChar; nextChar
          case JVM.invokespecial   => arg = nextChar
          case JVM.invokestatic    => arg = nextChar

          case JVM.new_        => arg = nextChar 
          case JVM.newarray    => arg = nextByte
          case JVM.anewarray   => arg = nextChar
          case JVM.arraylength => 
          case JVM.athrow      =>
          case JVM.checkcast   => arg = nextChar
          case JVM.instanceof  => arg = nextChar
          case JVM.monitorenter=> 
          case JVM.monitorexit =>
          case JVM.wide        => next(wide = true)
          case JVM.multianewarray => arg = nextChar; nextByte
          case JVM.ifnull      => arg = nextChar
          case JVM.ifnonnull   => arg = nextChar
          case JVM.goto_w      => arg = nextInt
          case JVM.jsr_w       => arg = nextInt
        }
        Instruction(offset, instr, arg)
      } else {
        Iterator.empty.next
      }
    }
  }

  case class Code(elems: Any*) {
    val size: Int = {
      elems map { 
        case b: Byte => 1
        case c: Char => 2
        case c: Code => c.size
      }
    }.sum
    
    def write() {
      elems foreach {
        case b: Byte => out.writeByte(b)
        case c: Char => out.writeChar(c)
        case c: Code => c.write()
      }
    }
  }

  class Method(val namestr: String, val flags: Int, val sigstr: String, 
               val maxStack: Int, val maxLocals: Int, val code: Code) {
    def this(template: MemberInfo, maxStack: Int, maxLocals: Int, code: Code) =
      this(
        template.name,
        template.flags & ~JAVA_ACC_ABSTRACT, 
        template.sig,
        maxStack,
        maxLocals,
        code)

    val name = Name(namestr)
    val sig = Name(external(sigstr))

    def write() {
      out.writeChar(flags)
      out.writeChar(pool.index(name))
      out.writeChar(pool.index(sig))
      out.writeChar(1) // attribute count
      out.writeChar(pool.codeTag)
      val attrlen = 12 + code.size
      out.writeInt(attrlen)
      out.writeChar(maxStack)
      out.writeChar(maxLocals)
      out.writeInt(code.size)
      code.write()
      out.writeChar(0) // exception table length
      out.writeChar(0) // # attributes
      
    }
  }

  def writeCountAndBlock(start: Int, end: Int, incr: Int) {
    out.writeChar(in.at(start)(in.nextChar) + incr)
    out.write(in.buf, start + 2, end - (start + 2))
  }

  case class Patch(offset: Int, op: () => Unit, skip: Int = 0)

  def incCharCountPatch(offset: Int, incr: Int) = 
    new Patch(offset, () => out.writeChar(in.at(offset)(in.nextChar) + incr), 2)

  def incIntCountPatch(offset: Int, incr: Int) = 
    new Patch(offset, () => out.writeInt(in.at(offset)(in.nextInt) + incr), 4)

  def writeClassFile(patches: Seq[Patch]) = {
    var written = 0
    for (p <- patches) {
      out.write(in.buf, written, p.offset - written)
      p.op()
      written = p.offset + p.skip
    }
    out.write(in.buf, written, in.buf.length - written)
  }

  def writeClassFile(fieldFixups: List[Field], methodFixups: List[Method], codePatches: List[Patch]) {
    pool.codeTag // force it.
    val patches = codePatches ++ List(
      incCharCountPatch(poolStart, pool.index.size),
      Patch(poolEnd, () => pool.writeNew()),
      incCharCountPatch(fieldsStart, fieldFixups.length),
      Patch(methodsStart, () => fieldFixups foreach (_.write())),
      incCharCountPatch(methodsStart, methodFixups.length),
      Patch(methodsEnd, () => methodFixups foreach (_.write()))) sortBy (_.offset)
    writeClassFile(patches)
    out.close()
  }
}

