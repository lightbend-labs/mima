package com.typesafe.tools.mima.core

import java.util.UUID

import scala.annotation.tailrec
import scala.collection.mutable, mutable.{ ArrayBuffer, ListBuffer }

import TastyFormat._, NameTags._, TastyTagOps._, TastyRefs._

object TastyUnpickler {
  def unpickleClass(in: TastyReader, clazz: ClassInfo, path: String): Unit = {
    val doPrint = false
    //val doPrint = path.contains("v1") && !path.contains("experimental2.tasty")
    //if (doPrint) TastyPrinter.printClassNames(in.fork, path)
    //if (doPrint) TastyPrinter.printPickle(in.fork, path)
    if (doPrint) unpickle(in, clazz, path)
  }

  def unpickle(in: TastyReader, clazz: ClassInfo, path: String): Unit = {
    readHeader(in)
    val names = readNames(in)
    val tree  = unpickleTree(getTreeReader(in, names), names)

    object trav extends Traverser {
      var pkgNames = List.empty[Name]
      val classes  = new mutable.ListBuffer[(Name, ClsDef)]

      override def traversePkg(pkg: Pkg): Unit = {
        val pkgName = pkg.path match {
          case TypeRefPkg(fullyQual) => fullyQual
          case p: UnknownPath        => SimpleName(p.show)
        }
        pkgNames ::= pkgName
        super.traversePkg(pkg)
        pkgNames match {
          case n :: ns => pkgNames = ns.ensuring(n == pkgName, s"last=$n pkgName=$pkgName")
          case _       => assert(false, s"Expected $pkgName as last pkg name, was empty")
        }
      }

      override def traverseClsDef(clsDef: ClsDef): Unit = {
        classes += ((pkgNames.headOption.getOrElse(nme.Empty), clsDef))
        super.traverseClsDef(clsDef)
      }
    }
    trav.traverse(tree)
    trav.classes.toList.foreach { case (pkgName, clsDef) =>
      if (pkgName.source == clazz.owner.fullName) {
        val cls = clazz.owner.classes.getOrElse(clsDef.name.source, NoClass)
        if (cls != NoClass) {
          cls._experimental |= clsDef.annots.exists(_.tycon.toString == "scala.annotation.experimental")
          cls._experimental |= clsDef.annots.exists(_.tycon.toString == "scala.annotation.experimental2")

          for (defDef <- clsDef.template.meths) {
            val isExperimental =
              defDef.annots.exists(_.tycon.toString == "scala.annotation.experimental") ||
              defDef.annots.exists(_.tycon.toString == "scala.annotation.experimental2")
            if (isExperimental)
              for (meth <- cls.lookupClassMethods(new MethodInfo(cls, defDef.name.source, 0, "()V")))
                meth._experimental = true
          }
        }
      }
    }
  }

  def unpickleTree(in: TastyReader, names: Names): Tree = {
    import in._

    def readName()         = names(readNat())
    def skipTree(tag: Int) = { skipTreeTagged(in, tag); UnknownTree(tag) }
    def readTypeRefPkg()   = TypeRefPkg(readName())                                 // fullyQualified_NameRef         -- A reference to a package member with given fully qualified name
    def readTypeRef()      = TypeRef(name = readName(), qual = readType())          // NameRef qual_Type              -- A reference `qual.name` to a non-local member
    def readAnnot()        = { readEnd(); Annot(readType(), skipTree(readByte())) } // tycon_Type fullAnnotation_Tree -- An annotation, given (class) type of constructor, and full application tree
    def readSharedType()   = unpickleTree(forkAt(readAddr()), names).asInstanceOf[Type]

    def readPath() = readByte() match {
      case TERMREFpkg => readTypeRefPkg()
      case tag        => skipTree(tag); UnknownPath(tag)
    }

    def readType(): Type = readByte() match {
      case TYPEREF    => readTypeRef()
      case TERMREFpkg => readTypeRefPkg()
      case SHAREDtype => readSharedType()
      case tag        => skipTree(tag); UnknownType(tag)
    }

    def readTree(): Tree = {
      val start = currentAddr
      val tag   = readByte()

      def processLengthTree() = {
        def readTrees(end: Addr) = until(end)(readTree()).filter(!_.isInstanceOf[UnknownTree])
        def readPackage()        = { val end = readEnd(); Pkg(readPath(), readTrees(end)) } // Path Tree* -- package path { topLevelStats }

        def nothingButMods(end: Addr) = currentAddr == end || isModifierTag(nextByte)

        def readDefDef() = {
          // Length NameRef Param* returnType_Term rhs_Term? Modifier*  -- modifiers def name [typeparams] paramss : returnType (= rhs)?
          // Param = TypeParam | TermParam
          val end  = readEnd()
          val name = readName()
          while (nextByte == TYPEPARAM || nextByte == PARAM || nextByte == EMPTYCLAUSE || nextByte == SPLITCLAUSE) skipTree(readByte()) // params
          skipTree(readByte()) // returnType
          if (!nothingButMods(end)) skipTree(readByte()) // rhs
          val annots = readAnnotsInMods(end)
          DefDef(name, annots)
        }

        def readTemplate() = {
          // TypeParam* TermParam* parent_Term* Self? Stat*              -- [typeparams] paramss extends parents { self => stats }, where Stat* always starts with the primary constructor.
          // TypeParam = TYPEPARAM   Length NameRef type_Term Modifier*  -- modifiers name bounds
          // TermParam = PARAM       Length NameRef type_Term Modifier*  -- modifiers name : type.
          //             EMPTYCLAUSE                                     -- an empty parameter clause ()
          //             SPLITCLAUSE                                     -- splits two non-empty parameter clauses of the same kind
          // Self      = SELFDEF     selfName_NameRef selfType_Term      -- selfName : selfType
          assert(readByte() == TEMPLATE)
          val end = readEnd()
          while (nextByte == TYPEPARAM)                                                   skipTree(readByte()) // vparams
          while (nextByte == PARAM || nextByte == EMPTYCLAUSE || nextByte == SPLITCLAUSE) skipTree(readByte()) // tparams
          while (nextByte != SELFDEF && nextByte != DEFDEF)                               skipTree(readByte()) // parents
          if (nextByte == SELFDEF) skipTree(readByte())                                                        // self
          val meths = new ListBuffer[DefDef]
          doUntil(end)(readByte match {
            case DEFDEF => meths += readDefDef()
            case tag    => skipTree(tag)
          })
          Template(meths.toList)
        }

        def readClassDef(name: Name, end: Addr) = ClsDef(name.toTypeName, readTemplate(), readAnnotsInMods(end)) // NameRef Template Modifier* -- modifiers class name template

        def readTypeMemberDef(end: Addr) = { goto(end); UnknownTree(TYPEDEF) } // NameRef type_Term Modifier* -- modifiers type name (= type | bounds)

        def readAnnotsInMods(end: Addr) = {
          val annots = new ListBuffer[Annot]
          doUntil(end)(readByte() match {
            case ANNOTATION                => annots += readAnnot()
            case tag if isModifierTag(tag) => skipTree(tag)
            case tag                       => assert(false, s"illegal modifier tag $tag at ${currentAddr.index - 1}, end = $end")
          })
          annots.toList
        }

        def readTypeDef() = {
          val end  = readEnd()
          val name = readName()
          if (nextByte == TEMPLATE) readClassDef(name, end) else readTypeMemberDef(end)
        }

        val end = fork.readEnd()
        val tree = tag match {
          case PACKAGE => readPackage()
          case TYPEDEF => readTypeDef()
          case _       => skipTree(tag)
        }
        softAssertEnd(end, s" start=$start tag=${astTagToString(tag)}")
        tree
      }

      astCategory(tag) match {
        case AstCat1TagOnly => skipTree(tag)
        case AstCat2Nat     => tag match {
          case TERMREFpkg => readTypeRefPkg()
          case _          => skipTree(tag)
        }
        case AstCat3AST     => readTree()
        case AstCat4NatAST  => tag match {
          case TYPEREF => readTypeRef()
          case _       => skipTree(tag)
        }
        case AstCat5Length  => processLengthTree()
      }
    }

    readTree()
  }

  def skipTreeTagged(in: TastyReader, tag: Int): Unit = {
    import in._
    astCategory(tag) match {
      case AstCat1TagOnly =>
      case AstCat2Nat     => readNat()
      case AstCat3AST     => skipTreeTagged(in, in.readByte())
      case AstCat4NatAST  => readNat(); skipTreeTagged(in, in.readByte())
      case AstCat5Length  => goto(readEnd())
    }
  }

  sealed trait Tree extends ShowSelf

  final case class UnknownTree(tag: Int) extends Tree {
    val id = unknownTreeId.getAndIncrement()
    def show = s"UnknownTree(${astTagToString(tag)})" //+ s"#$id"
  }
  var unknownTreeId = new java.util.concurrent.atomic.AtomicInteger()

  final case class Pkg(path: Path, trees: List[Tree]) extends Tree { def show = s"package $path${trees.map("\n  " + _).mkString}" }

  final case class ClsDef(name: TypeName, template: Template, annots: List[Annot]) extends Tree { def show = s"${annots.map("" + _ + " ").mkString}class $name$template" }
  final case class Template(meths: List[DefDef]) extends Tree { def show = s"${meths.map("\n  " + _).mkString}" }
  final case class DefDef(name: Name, annots: List[Annot] = Nil) extends Tree { def show = s"${annots.map("" + _ + " ").mkString} def $name" }

  sealed trait Type extends Tree
  final case class UnknownType(tag: Int)           extends Type { def show = s"UnknownType(${astTagToString(tag)})" }
  final case class TypeRef(qual: Type, name: Name) extends Type { def show = s"$qual.$name" }

  sealed trait Path extends Type
  final case class UnknownPath(tag: Int)       extends Path { def show = s"UnknownPath(${astTagToString(tag)})" }
  final case class TypeRefPkg(fullyQual: Name) extends Path { def show = s"$fullyQual" }

  final case class Annot(tycon: Type, fullAnnotation: Tree) extends Tree { def show = s"@$tycon" }

  sealed class Traverser {
    def traverse(tree: Tree): Unit = tree match {
      case pkg: Pkg        => traversePkg(pkg)
      case clsDef: ClsDef  => traverseClsDef(clsDef)
      case tmpl: Template  => traverseTemplate(tmpl)
      case defDef: DefDef  => traverseDefDef(defDef)
      case tp: Type        => traverseType(tp)
      case annot: Annot    => traverseAnnot(annot)
      case UnknownTree(_)  =>
    }

    def traverseName(name: Name) = ()

    def traverseTrees(trees: List[Tree])    = trees.foreach(traverse)
    def traverseAnnots(annots: List[Annot]) = annots.foreach(traverse)

    def traversePkg(pkg: Pkg)               = { traverse(pkg.path); traverseTrees(pkg.trees) }
    def traverseClsDef(clsDef: ClsDef)      = { traverseName(clsDef.name); traverseAnnots(clsDef.annots) }
    def traverseTemplate(tmpl: Template)    = { traverseTrees(tmpl.meths) }
    def traverseDefDef(defDef: DefDef)      = { traverseName(defDef.name); traverseAnnots(defDef.annots) }
    def traverseAnnot(annot: Annot)         = { traverse(annot.tycon); traverse(annot.fullAnnotation) }

    def traversePath(path: Path) = path match {
      case TypeRefPkg(fullyQual) => traverseName(fullyQual)
      case UnknownPath(_)        =>
    }

    def traverseType(tp: Type): Unit = tp match {
      case path: Path          => traversePath(path)
      case TypeRef(qual, name) => traverse(qual); traverseName(name)
      case UnknownType(_)      =>
    }
  }

  class ForeachTraverser(f: Tree => Unit) extends Traverser {
    override def traverse(t: Tree) = { f(t); super.traverse(t) }
  }

  class FilterTraverser(p: Tree => Boolean) extends Traverser {
    val hits = mutable.ListBuffer[Tree]()
    override def traverse(t: Tree) = { if (p(t)) hits += t; super.traverse(t) }
  }

  class CollectTraverser[T](pf: PartialFunction[Tree, T]) extends Traverser {
    val results = mutable.ListBuffer[T]()
    override def traverse(tree: Tree): Unit = { pf.runWith(results += _)(tree); super.traverse(tree) }
  }

  def getTreeReader(in: TastyReader, names: Names): TastyReader = {
    getSectionReader(in, names, ASTsSection).getOrElse(sys.error(s"No $ASTsSection section?!"))
  }

  def getSectionReader(in: TastyReader, names: Names, name: String): Option[TastyReader] = {
    import in._
    @tailrec def loop(): Option[TastyReader] = {
      if (isAtEnd) None
      else if (names(readNat()).debug == name) Some(nextSectionReader(in))
      else { goto(readEnd()); loop() }
    }
    loop()
  }

  private def nextSectionReader(in: TastyReader) = {
    import in._
    val end  = readEnd()
    val curr = currentAddr
    goto(end)
    new TastyReader(bytes, curr.index, end.index, curr.index)
  }

  def readNames(in: TastyReader): Names = {
    val names = new ArrayBuffer[Name]
    in.doUntil(in.readEnd())(names += readNewName(in, names))
    names.toIndexedSeq
  }

  private def readNewName(in: TastyReader, names: ArrayBuffer[Name]): Name = {
    import in._

    val tag = readByte()
    val end = readEnd()

    def nameAtIdx(idx: Int) = try names(idx) catch {
      case e: ArrayIndexOutOfBoundsException =>
        throw new Exception(s"trying to read name @ idx=$idx tag=${nameTagToString(tag)}", e)
    }

    def readName() = nameAtIdx(readNat())

    def readParamSig(): ParamSig[ErasedTypeRef] = {
      val ref = readInt()
      Either.cond(ref >= 0, ErasedTypeRef(nameAtIdx(ref)), ref.abs)
    }

    def readSignedRest(orig: Name, target: Name) = {
      SignedName(orig, new MethodSignature(result = ErasedTypeRef(readName()), params = until(end)(readParamSig())), target)
    }

    val name = tag match {
      case UTF8           => val start = currentAddr; goto(end); SimpleName(new String(bytes.slice(start.index, end.index), "UTF-8"))
      case QUALIFIED      => QualifiedName(readName(),         PathSep, readName().asSimpleName)
      case EXPANDED       => QualifiedName(readName(),     ExpandedSep, readName().asSimpleName)
      case EXPANDPREFIX   => QualifiedName(readName(), ExpandPrefixSep, readName().asSimpleName)

      case UNIQUE         => UniqueName(sep = readName().asSimpleName, num = readNat(), qual = ifBefore(end)(readName(), Empty))
      case DEFAULTGETTER  => DefaultName(readName(), readNat())

      case SUPERACCESSOR  => PrefixName(SuperPrefix, readName())
      case INLINEACCESSOR => PrefixName(InlinePrefix, readName())
      case BODYRETAINER   => SuffixName(readName(), BodyRetainerSuffix)
      case OBJECTCLASS    => ObjectName(readName())

      case SIGNED         => val name = readName(); readSignedRest(name, name)
      case TARGETSIGNED   => readSignedRest(readName(), readName())

      case _              => sys.error(s"at NameRef(${names.size}): name `${readName().debug}` is qualified by tag ${nameTagToString(tag)}")
    }
    assertEnd(end, s" bad name=${name.debug}")
    name
  }

  type Names = IndexedSeq[Name]

  sealed abstract class Name extends ShowSelf {
    final def asSimpleName = this match {
      case x: SimpleName => x
      case _             => throw new AssertionError(s"not simplename: $debug")
    }

    final def toTermName = this match {
      case TypeName(name) => name
      case name           => name
    }

    final def isObjectName = this.isInstanceOf[ObjectName]
    final def toTypeName   = TypeName(this)
    final def source       = SourceEncoder.encode(this)
    final def debug        = DebugEncoder.encode(this)

    final def show = source
  }

  final case class SimpleName(raw: String)                                                   extends Name
  final case class ObjectName(base: Name)                                                    extends Name
  final case class TypeName private (base: Name)                                             extends Name
  final case class QualifiedName(qual: Name, sep: SimpleName, sel: SimpleName)               extends Name

  final case class UniqueName(qual: Name, sep: SimpleName, num: Int)                         extends Name
  final case class DefaultName(qual: Name, num: Int)                                         extends Name

  final case class PrefixName(pref: SimpleName, qual: Name)                                  extends Name
  final case class SuffixName(qual: Name, suff: SimpleName)                                  extends Name

  final case class SignedName(qual: Name, sig: MethodSignature[ErasedTypeRef], target: Name) extends Name

  val Empty              = SimpleName("")
  val PathSep            = SimpleName(".")
  val ExpandedSep        = SimpleName("$$")
  val ExpandPrefixSep    = SimpleName("$")
  val InlinePrefix       = SimpleName("inline$")
  val SuperPrefix        = SimpleName("super$")
  val BodyRetainerSuffix = SimpleName("$retainedBody")
  val Constructor        = SimpleName("<init>")

  object nme {
    def NoSymbol = SimpleName("NoSymbol")
    def Empty    = SimpleName("<empty>")
  }

  type ParamSig[T] = Either[Int, T]

  sealed abstract class Signature[+T] {
    final def show = this match {
      case MethodSignature(params, result) => params.map(_.merge).mkString("(", ",", ")") + result
    }
  }

  final case class MethodSignature[T](params: List[ParamSig[T]], result: T) extends Signature[T] {
    def map[U](f: T => U): MethodSignature[U] = this match {
      case MethodSignature(params, result) => MethodSignature(params.map(_.map(f)), f(result))
    }
  }

  final case class ErasedTypeRef(qualifiedName: TypeName, arrayDims: Int) {
    def signature: String = {
      val qualified = qualifiedName.source
      val pref      = if (qualifiedName.toTermName.isObjectName) "object " else ""
      s"${"[" * arrayDims}$pref$qualified"
    }
  }

  object ErasedTypeRef {
    private val InnerRegex = """(.*)\[\]""".r

    def apply(tname: Name): ErasedTypeRef = {
      def name(qual: Name, tname: SimpleName, isModule: Boolean) = {
        val qualified = if (qual == Empty) tname else QualifiedName(qual, PathSep, tname)
        if (isModule) ObjectName(qualified) else qualified
      }
      def specialised(qual: Name, terminal: String, isModule: Boolean, arrayDims: Int = 0): ErasedTypeRef = terminal match {
        case InnerRegex(inner) => specialised(qual, inner, isModule, arrayDims + 1)
        case clazz             => ErasedTypeRef(name(qual, SimpleName(clazz), isModule).toTypeName, arrayDims)
      }
      def transform(name: Name, isModule: Boolean = false): ErasedTypeRef = name match {
        case ObjectName(classKind)             => transform(classKind, isModule = true)
        case SimpleName(raw)                   => specialised(Empty, raw, isModule) // unqualified in the <empty> package
        case QualifiedName(path, PathSep, sel) => specialised(path, sel.raw, isModule)
        case _                                 => throw new AssertionError(s"Unexpected erased type ref ${name.debug}")
      }
      transform(tname.toTermName)
    }
  }

  sealed trait NameEncoder[U] {
    final def encode[O](name: Name)(init: => U, finish: U => O) = finish(traverse(init, name))
    def traverse(u: U, name: Name): U
  }

  sealed trait StringBuilderEncoder extends NameEncoder[StringBuilder] {
    final def encode(name: Name) = name match {
      case SimpleName(raw) => raw
      case _               => super.encode(name)(new StringBuilder(25), _.toString)
    }
  }

  object SourceEncoder extends StringBuilderEncoder {
    def traverse(sb: StringBuilder, name: Name): StringBuilder = name match {
      case Constructor                   => sb
      case SimpleName(raw)               => sb ++= raw
      case ObjectName(base)              => traverse(sb, base)
      case TypeName(base)                => traverse(sb, base)
      case SignedName(qual, _, _)        => traverse(sb, qual)
      case UniqueName(qual, sep, num)    => traverse(sb, qual) ++= sep.raw ++= s"$num"
      case QualifiedName(qual, sep, sel) => traverse(sb, qual) ++= sep.raw ++= sel.raw
      case PrefixName(pref, qual)        => traverse(sb ++= pref.raw, qual)
      case SuffixName(qual, suff)        => traverse(sb, qual) ++= suff.raw
      case DefaultName(qual, num)        => traverse(sb, qual) ++= s"$$default$$${num + 1}"
    }
  }

  object DebugEncoder extends StringBuilderEncoder {
    def traverse(sb: StringBuilder, name: Name): StringBuilder = name match {
      case SimpleName(raw)                => sb ++= raw
      case DefaultName(qual, num)         => traverse(sb, qual) ++= "[Default " ++= s"${num + 1}" += ']'
      case PrefixName(pref, qual)         => traverse(sb, qual) ++= "[Prefix " ++= pref.raw += ']'
      case SuffixName(qual, suff)         => traverse(sb, qual) ++= "[Suffix " ++= suff.raw += ']'
      case ObjectName(base)               => traverse(sb, base) ++= "[ModuleClass]"
      case TypeName(base)                 => traverse(sb, base) ++= "[Type]"
      case SignedName(name, sig, target)  => traverse(sb, name) ++= "[Signed (" ++= sig.map(_.signature).show ++= " @" ++= target.source += ']'
      case QualifiedName(qual, sep, name) => traverse(sb, qual) ++= "[Qualified " ++= sep.raw += ' ' ++= name.raw += ']'
      case UniqueName(qual, sep, num)     => traverse(sb, qual) ++= "[Unique " ++= sep.raw += ' ' ++= s"$num" += ']'
    }
  }

  final case class Header(
      val header: (Int, Int, Int, Int),
      val version: (Int, Int, Int),
      val toolingVersion: String,
      val uuid: UUID,
  )

  def readHeader(in: TastyReader) = {
    import in._

    def readToolingVersion() = {
      val toolingLen = readNat()
      val toolingVersion = new String(bytes, currentAddr.index, toolingLen)
      goto(currentAddr + toolingLen)
      toolingVersion
    }

    Header(
      header         = (readByte(), readByte(), readByte(), readByte()),
      version        = (readNat(), readNat(), readNat()),
      toolingVersion = readToolingVersion(),
      uuid           = new UUID(readUncompressedLong(), readUncompressedLong()),
    )
  }

  trait ShowSelf extends Any {
    def show: String
    override def toString = show
  }
}
