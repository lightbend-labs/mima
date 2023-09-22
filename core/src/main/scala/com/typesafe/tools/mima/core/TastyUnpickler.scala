package com.typesafe.tools.mima.core

import java.util.UUID

import scala.annotation.{nowarn, tailrec}
import scala.collection.mutable, mutable.{ ArrayBuffer, ListBuffer }

import TastyFormat._, NameTags._, TastyTagOps._, TastyRefs._

object TastyUnpickler {
  def unpickleClass(in: TastyReader, clazz: ClassInfo, path: String): Unit = {
    val doPrint = false
    //val doPrint = true
    //val doPrint = path.contains("v1") && !path.contains("exclude.tasty")
    //if (doPrint) TastyPrinter.printClassNames(in.fork, path)
    if (doPrint) TastyPrinter.printPickle(in.fork, path)

    readHeader(in)
    val names = readNames(in)
    val tree  = unpickleTree(getTreeReader(in, names), names)

    copyPrivateWithin(tree, clazz.owner)
    copyAnnotations(tree, clazz.owner)
  }

  private abstract class ClassTraverser(pkgInfo: PackageInfo) extends Traverser {
    var pkgNames = List.empty[Name]
    var clsNames = List.empty[Name]

    override def traversePkg(pkg: Pkg): Unit = {
      pkgNames ::= getPkgName(pkg)
      super.traversePkg(pkg)
      pkgNames = pkgNames.tail
    }

    override def traverseClsDef(clsDef: ClsDef): Unit = {
      clsNames ::= clsDef.name
      val cls = currentClass
      if (cls != NoClass) forEachClass(clsDef, cls)
      super.traverseClsDef(clsDef)
      clsNames = clsNames.tail
    }

    def getPkgName(pkg: Pkg) = pkg.path match {
      case TypeRefPkg(fullyQual) => fullyQual
      case p: UnknownPath        => SimpleName(p.show)
    }

    def currentClass: ClassInfo = {
      val pkgName = pkgNames.headOption.getOrElse(nme.Empty)
      if (pkgName.source == pkgInfo.fullName) {
        val clsName0 = clsNames.reverseIterator.mkString("$")
        val clsName = clsNames match {
          case TypeName(ObjectName(_)) :: _ => clsName0 + "$"
          case _                            => clsName0
        }
        pkgInfo.classes.getOrElse(clsName, NoClass)
      } else NoClass
    }

    def forEachClass(clsDef: ClsDef, cls: ClassInfo): Unit
  }

  def copyAnnotations(tree: Tree, pkgInfo: PackageInfo): Unit = new ClassTraverser(pkgInfo) {
    def forEachClass(clsDef: ClsDef, cls: ClassInfo): Unit = {
      cls._annotations ++= clsDef.annots.map(annot => AnnotInfo(annot.tycon.toString))

      for (defDef <- clsDef.template.meths) {
        val annots = defDef.annots.map(annot => AnnotInfo(annot.tycon.toString))
        for (meth <- cls.lookupClassMethods(new MethodInfo(cls, defDef.name.source, 0, "()V")))
          meth._annotations ++= annots
      }
    }
  }.traverse(tree)

  def copyPrivateWithin(tree: Tree, pkgInfo: PackageInfo): Unit = new ClassTraverser(pkgInfo) {
    override def forEachClass(clsDef: ClsDef, cls: ClassInfo): Unit =
      clsDef.privateWithin.foreach(_ => cls.module._scopedPrivate = true)

    override def traverseTemplate(tmpl: Template): Unit = {
      super.traverseTemplate(tmpl)
      doMethods(tmpl)
    }

    def doMethods(tmpl: Template) = {
      val clazz = currentClass
      (tmpl.fields ::: tmpl.meths).iterator
        .filter(_.name != Name.Constructor) // TODO support package private constructors
        .toSeq.groupBy(_.name).foreach { case (name, pickleMethods) =>
          doMethodOverloads(clazz, name, pickleMethods)
        }
    }

    def doMethodOverloads(clazz: ClassInfo, name: Name, pickleMethods: Seq[TermMemberDef]) = {
      val bytecodeMethods = clazz.methods.get(name.source).filter(!_.isBridge).toList
      if (pickleMethods.size == bytecodeMethods.size && pickleMethods.exists(_.privateWithin.isDefined)) {
        bytecodeMethods.zip(pickleMethods).foreach { case (bytecodeMeth, pickleMeth) =>
          bytecodeMeth.scopedPrivate = pickleMeth.privateWithin.isDefined
        }
      }
    }
  }.traverse(tree)

  def unpickleTree(in: TastyReader, names: Names): Tree = {
    import in._

    def readName()         = names(readNat())
    def skipTree(tag: Int) = { skipTreeTagged(in, tag); UnknownTree(tag) }
    def readTypeRefPkg()   = TypeRefPkg(readName())                                 // fullyQualified_NameRef          -- A reference to a package member with given fully qualified name
    def readTypeRef()      = TypeRef(name = readName(), qual = readType())          // NameRef qual_Type               -- A reference `qual.name` to a non-local member
    def readAnnot()        = { readEnd(); Annot(readType(), skipTree(readByte())) } // tycon_Type fullAnnotation_Tree  -- An annotation, given (class) type of constructor, and full application tree
    def readSharedType()   = unpickleTree(forkAt(readAddr()), names) match {
      case t @ UnknownTree(tag) => throw new Exception(s"Expected Type, but was UnknownTree(${astTagToString(tag)}); init:${t.stack.map("\n\tat " + _).mkString}\n")
      case tree                 => tree.asInstanceOf[Type]
    }

    def readPath() = readByte() match {
      case TERMREFpkg => readTypeRefPkg()
      case TYPEREFpkg => readTypeRefPkg()
      case tag        => skipTree(tag); UnknownPath(tag)
    }

    def readType(): Type = readByte() match {
      case TERMREFpkg => readTypeRefPkg()
      case TYPEREFpkg => readTypeRefPkg()
      case TYPEREF    => readTypeRef()
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

        def readValDef() = {
          // Length NameRef type_Term rhs_Term? Modifier*  -- modifiers val name : type (= rhs)?
          val end  = readEnd()
          val name = readName()
          skipTree(readByte()) // type
          if (!nothingButMods(end)) skipTree(readByte()) // rhs
          val (privateWithin, annots) = readMods(end)
          ValDef(name, privateWithin, annots)
        }

        def readDefDef() = {
          // Length NameRef Param* returnType_Term rhs_Term? Modifier*  -- modifiers def name [typeparams] paramss : returnType (= rhs)?
          // Param = TypeParam | TermParam
          val end  = readEnd()
          val name = readName()
          while (nextByte == TYPEPARAM || nextByte == PARAM || nextByte == EMPTYCLAUSE || nextByte == SPLITCLAUSE) skipTree(readByte()) // params
          skipTree(readByte()) // returnType
          if (!nothingButMods(end)) skipTree(readByte()) // rhs
          val (privateWithin, annots) = readMods(end)
          DefDef(name, privateWithin, annots)
        }

        def readTemplate(): Template = {
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
          val classes = new ListBuffer[ClsDef]
          val fields = new ListBuffer[ValDef]
          val meths = new ListBuffer[DefDef]
          doUntil(end)(readByte() match {
            case TYPEDEF => readTypeDef() match { case clsDef: ClsDef => classes += clsDef case _ => }
            case  VALDEF => fields += readValDef()
            case  DEFDEF => meths  += readDefDef()
            case tag     => skipTree(tag)
          })
          Template(classes.toList, fields.toList, meths.toList)
        }

        def readClassDef(name: Name, end: Addr) = {
          // NameRef Template Modifier* -- modifiers class name template
          val template = readTemplate()
          val (privateWithin, annots) = readMods(end)
          ClsDef(name.toTypeName, template, privateWithin, annots)
        }

        def readTypeMemberDef(end: Addr) = { goto(end); UnknownTree(TYPEDEF) } // NameRef type_Term Modifier* -- modifiers type name (= type | bounds)

        def readMods(end: Addr): (Option[Type], List[Annot]) = {
          //   PRIVATEqualified qualifier_Type --   private[qualifier]
          // PROTECTEDqualified qualifier_Type -- protected[qualifier]
          var privateWithin = Option.empty[Type]
          val annots = new ListBuffer[Annot]
          doUntil(end)(readByte() match {
            case ANNOTATION                => annots += readAnnot()
            case PRIVATEqualified          => privateWithin = Some(readType())
            case PROTECTEDqualified        => privateWithin = Some(readType())
            case tag if isModifierTag(tag) => skipTree(tag)
            case tag                       => assert(false, s"illegal modifier tag ${astTagToString(tag)} at ${currentAddr.index - 1}, end = $end")
          })
          (privateWithin, annots.toList)
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
          case TYPEREFpkg => readTypeRefPkg()
          case _          => skipTree(tag)
        }
        case AstCat3AST     => readTree()
        case AstCat4NatAST  => tag match {
          case TYPEREFsymbol => skipTree(tag) match { case UnknownTree(tag) => UnknownType(tag) }
          case TYPEREF       => readTypeRef()
          case _             => skipTree(tag)
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
    val stack = new Exception().getStackTrace
    val id = unknownTreeId.getAndIncrement()
    def show = s"UnknownTree(${astTagToString(tag)})" //+ s"#$id"
  }
  var unknownTreeId = new java.util.concurrent.atomic.AtomicInteger()

  final case class Pkg(path: Path, trees: List[Tree]) extends Tree { def show = s"package $path${trees.map("\n  " + _).mkString}" }

  final case class ClsDef(name: TypeName, template: Template, privateWithin: Option[Type], annots: List[Annot]) extends Tree {
    def show = s"${showXs(annots, end = " ")}${showPrivateWithin(privateWithin)}class $name$template"
  }
  final case class Template(classes: List[ClsDef], fields: List[ValDef], meths: List[DefDef]) extends Tree { def show = s"${(classes ::: meths).map("\n  " + _).mkString}" }
  final case class ValDef(name: Name, privateWithin: Option[Type], annots: List[Annot] = Nil) extends TermMemberDef
  final case class DefDef(name: Name, privateWithin: Option[Type], annots: List[Annot] = Nil) extends TermMemberDef

  sealed trait TermMemberDef extends Tree {
    def name: Name; def privateWithin: Option[Type]; def annots: List[Annot]
    def show = s"${showXs(annots, end = " ")}${showPrivateWithin(privateWithin)}def $name"
  }

  sealed trait Type extends Tree
  final case class UnknownType(tag: Int)           extends Type { def show = s"UnknownType(${astTagToString(tag)})" }
  final case class TypeRef(qual: Type, name: Name) extends Type { def show = s"$qual.$name" }

  sealed trait Path extends Type
  final case class UnknownPath(tag: Int)       extends Path { def show = s"UnknownPath(${astTagToString(tag)})" }
  final case class TypeRefPkg(fullyQual: Name) extends Path { def show = s"$fullyQual"                          }

  final case class Annot(tycon: Type, fullAnnotation: Tree) extends Tree { def show = s"@$tycon" }

  def showPrivateWithin(privateWithin: Option[Type]): String = privateWithin match {
    case Some(privateWithin) => s"private[${privateWithin.show}] "
    case _                   => ""
  }

  sealed class Traverser {
    def traverse(tree: Tree): Unit = tree match {
      case pkg: Pkg        => traversePkg(pkg)
      case clsDef: ClsDef  => traverseClsDef(clsDef)
      case tmpl: Template  => traverseTemplate(tmpl)
      case valDef: ValDef  => traverseValDef(valDef)
      case defDef: DefDef  => traverseDefDef(defDef)
      case tp: Type        => traverseType(tp)
      case annot: Annot    => traverseAnnot(annot)
      case UnknownTree(_)  =>
    }

    def traverseName(name: Name) = ()

    def traversePkg(pkg: Pkg)                              = { val Pkg(path, trees) = pkg; traverse(path); trees.foreach(traverse) }
    def traverseClsDef(clsDef: ClsDef)                     = { val ClsDef(name, tmpl, privateWithin, annots) = clsDef; traverseName(name); traverseTemplate(tmpl); traversePrivateWithin(privateWithin); annots.foreach(traverse) }
    def traverseTemplate(tmpl: Template)                   = { val Template(classes, fields, meths) = tmpl; classes.foreach(traverse); fields.foreach(traverse); meths.foreach(traverse) }
    def traverseValDef(valDef: ValDef)                     = { val ValDef(name, privateWithin, annots) = valDef; traverseName(name); traversePrivateWithin(privateWithin); annots.foreach(traverse) }
    def traverseDefDef(defDef: DefDef)                     = { val DefDef(name, privateWithin, annots) = defDef; traverseName(name); traversePrivateWithin(privateWithin); annots.foreach(traverse) }
    def traversePrivateWithin(privateWithin: Option[Type]) = { privateWithin.foreach(traverseType) }
    def traverseAnnot(annot: Annot)                        = { val Annot(tycon, fullAnnotation) = annot; traverse(tycon); traverse(fullAnnotation) }

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
      case QUALIFIED      => QualifiedName(readName(),         Name.PathSep, readName().asSimpleName)
      case EXPANDED       => QualifiedName(readName(),     Name.ExpandedSep, readName().asSimpleName)
      case EXPANDPREFIX   => QualifiedName(readName(), Name.ExpandPrefixSep, readName().asSimpleName)

      case UNIQUE         => UniqueName(sep = readName().asSimpleName, num = readNat(), qual = ifBefore(end)(readName(), Name.Empty))
      case DEFAULTGETTER  => DefaultName(readName(), readNat())

      case SUPERACCESSOR  => PrefixName( Name.SuperPrefix, readName())
      case INLINEACCESSOR => PrefixName(Name.InlinePrefix, readName())
      case BODYRETAINER   => SuffixName(readName(), Name.BodyRetainerSuffix)
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
  @nowarn("msg=constructor modifiers are assumed by synthetic")
  final case class TypeName private[TastyUnpickler] (base: Name)                             extends Name
  final case class QualifiedName(qual: Name, sep: SimpleName, sel: SimpleName)               extends Name

  final case class UniqueName(qual: Name, sep: SimpleName, num: Int)                         extends Name
  final case class DefaultName(qual: Name, num: Int)                                         extends Name

  final case class PrefixName(pref: SimpleName, qual: Name)                                  extends Name
  final case class SuffixName(qual: Name, suff: SimpleName)                                  extends Name

  final case class SignedName(qual: Name, sig: MethodSignature[ErasedTypeRef], target: Name) extends Name

  object Name {
    val Empty              = SimpleName("")
    val PathSep            = SimpleName(".")
    val ExpandedSep        = SimpleName("$$")
    val ExpandPrefixSep    = SimpleName("$")
    val InlinePrefix       = SimpleName("inline$")
    val SuperPrefix        = SimpleName("super$")
    val BodyRetainerSuffix = SimpleName("$retainedBody")
    val Constructor        = SimpleName("<init>")
  }

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
        val qualified = if (qual == Name.Empty) tname else QualifiedName(qual, Name.PathSep, tname)
        if (isModule) ObjectName(qualified) else qualified
      }
      def specialised(qual: Name, terminal: String, isModule: Boolean, arrayDims: Int = 0): ErasedTypeRef = terminal match {
        case InnerRegex(inner) => specialised(qual, inner, isModule, arrayDims + 1)
        case clazz             => ErasedTypeRef(name(qual, SimpleName(clazz), isModule).toTypeName, arrayDims)
      }
      def transform(name: Name, isModule: Boolean = false): ErasedTypeRef = name match {
        case ObjectName(classKind)                  => transform(classKind, isModule = true)
        case SimpleName(raw)                        => specialised(Name.Empty, raw, isModule) // unqualified in the <empty> package
        case QualifiedName(path, Name.PathSep, sel) => specialised(path, sel.raw, isModule)
        case _                                      => throw new AssertionError(s"Unexpected erased type ref ${name.debug}")
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
      case Name.Constructor              => sb
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

  def readHeader(in: TastyReader): Header = {
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

  def showXs[X <: ShowSelf](xs: List[X], start: String = "", sep: String = " ", end: String = ""): String =
    if (xs.isEmpty) ""
    else xs.iterator.map(_.show).mkString(start, sep, end)
}
