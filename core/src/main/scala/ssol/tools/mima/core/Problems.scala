package ssol.tools.mima.core

object Problem {
  object Status extends Enumeration {
    val Unfixable = Value("unfixable")
    val Upgradable = Value("upgradable") // means MiMa Client can fix the bytecode
    val Ignored = Value("ignored")
  }

  object ClassVersion extends Enumeration {
    val New = Value("new")
    val Old = Value("old")
  }
}

sealed abstract class Problem {
  var status = Problem.Status.Unfixable
  var affectedVersion = Problem.ClassVersion.New
  val fileName: String
  def description: String
  def referredMember: String
}

case class MissingFieldProblem(oldfld: MemberInfo) extends Problem {
  override val fileName: String = oldfld.owner.sourceFileName
  override def description = oldfld.fieldString + " does not have a correspondent in " + affectedVersion + " version"
  override def referredMember = oldfld.fullName
}

case class MissingMethodProblem(meth: MemberInfo) extends Problem {
  override val fileName: String = meth.owner.sourceFileName
  override def description = (if (meth.isDeferred && !meth.owner.isTrait) "abstract " else "") + meth.methodString + " does not have a correspondent in " + affectedVersion + " version"
  override def referredMember = meth.fullName
}

case class UpdateForwarderBodyProblem(meth: MemberInfo) extends Problem {
  assert(meth.owner.isTrait)
  assert(meth.owner.hasStaticImpl(meth))

  status = Problem.Status.Upgradable
  override val fileName: String = meth.owner.sourceFileName
  override def description = "classes mixing " + meth.owner.fullName + " needs to update body of " + meth.shortMethodString
  override def referredMember = meth.fullName
}

case class MissingClassProblem(oldclazz: ClassInfo) extends Problem {
  override val fileName: String = oldclazz.sourceFileName
  override def description = oldclazz.classString + " does not have a correspondent in " + affectedVersion + " version"
  override def referredMember = oldclazz.shortDescription
}

case class AbstractClassProblem(oldclazz: ClassInfo) extends Problem {
  override val fileName: String = oldclazz.sourceFileName
  override def description = oldclazz.classString + " was concrete; is declared abstract in " + affectedVersion + " version"
  override def referredMember = oldclazz.shortDescription
}

case class FinalClassProblem(oldclazz: ClassInfo) extends Problem {
  override val fileName: String = oldclazz.sourceFileName
  override def description = oldclazz.classString + " is declared final in " + affectedVersion + " version"
  override def referredMember = oldclazz.shortDescription
}

case class FinalMethodProblem(newmemb: MemberInfo) extends Problem {
  override val fileName: String = newmemb.owner.sourceFileName
  override def description = newmemb.methodString + " is declared final in " + affectedVersion + " version"
  override def referredMember = newmemb.fullName
}

case class IncompatibleFieldTypeProblem(oldfld: MemberInfo, newfld: MemberInfo) extends Problem {
  override val fileName: String = oldfld.owner.sourceFileName
  override def description = newfld.fieldString + "'s type has changed; was: " + oldfld.tpe + ", is now: " + newfld.tpe
  override def referredMember = oldfld.fullName
}

case class IncompatibleMethTypeProblem(oldmeth: MemberInfo, newmeths: List[MemberInfo]) extends Problem {
  override val fileName: String = oldmeth.owner.sourceFileName

  override val description = {
    oldmeth.methodString + (if (newmeths.tail.isEmpty)
      "'s type has changed; was " + oldmeth.tpe + ", is now: " + newmeths.head.tpe
    else
      " does not have a correspondent with same parameter signature among " +
        (newmeths map (_.tpe) mkString ", "))
  }

  override def referredMember = oldmeth.fullName
}

case class IncompatibleResultTypeProblem(oldmeth: MemberInfo, newmeth: MemberInfo) extends Problem {
  override val fileName: String = oldmeth.owner.sourceFileName

  override val description = {
    oldmeth.methodString + " has now a different result type; was: " +
      oldmeth.tpe.resultType + ", is now: " + newmeth.tpe.resultType
  }

  override def referredMember = oldmeth.fullName
}

case class AbstractMethodProblem(newmeth: MemberInfo) extends Problem {
  status = Problem.Status.Upgradable
  override val fileName: String = newmeth.owner.sourceFileName
  override def description = "abstract " + newmeth.methodString + " does not have a correspondent in " + affectedVersion + " version"
  override def referredMember = newmeth.fullName
}

case class IncompatibleTemplateDefProblem(oldClazz: ClassInfo, newClazz: ClassInfo) extends Problem {
  override val fileName: String = oldClazz.sourceFileName
  override val description = {
    "declaration of " + oldClazz.description + " has changed to " + newClazz.description +
      " in new version; changing " + oldClazz.declarationPrefix + " to " + newClazz.declarationPrefix + " breaks client code"
  }
  override def referredMember = oldClazz.shortDescription
}

case class MissingTypesProblem(newClazz: ClassInfo, missing: Iterable[ClassInfo]) extends Problem {
  override val fileName: String = newClazz.sourceFileName
  override val description = "the type hierarchy of " + newClazz.description + " has changed in new version. " +
    "Missing types " + missing.map(_.fullName).mkString("{", ",", "}")
  override def referredMember = newClazz.shortDescription
}

case class CyclicTypeReferenceProblem(clz: ClassInfo) extends Problem {
  override val fileName: String = clz.sourceFileName
  override val description = {
    "the type hierarchy of " + clz.description + " has changed in new version. Type " + clz.name + " appears to be a subtype of itself"
  }
  override def referredMember = clz.shortDescription
}

case class InaccessibleFieldProblem(newfld: MemberInfo) extends Problem {
  override val fileName: String = newfld.owner.sourceFileName
  override def description = newfld.fieldString + " was public; is inaccessible in " + affectedVersion + " version"
  override def referredMember = newfld.fullName
}

case class InaccessibleMethodProblem(newmeth: MemberInfo) extends Problem {
  override val fileName: String = newmeth.owner.sourceFileName
  override def description = newmeth.methodString + " was public; is inaccessible in " + affectedVersion + " version"
  override def referredMember = newmeth.fullName
}

case class InaccessibleClassProblem(newclazz: ClassInfo) extends Problem {
  override val fileName: String = newclazz.sourceFileName
  override def description = newclazz.classString + " was public; is inaccessible in " + affectedVersion + " version"
  override def referredMember = newclazz.shortDescription
}

