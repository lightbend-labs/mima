package com.typesafe.tools.mima.core

object Problem {
  object ClassVersion extends Enumeration {
    val New = Value("new")
    val Old = Value("old")
  }
}

trait ProblemRef {
  type Ref
  def ref: Ref
  def fileName: String
  def referredMember: String

  // name that can be used to write a matching filter
  def matchName: Option[String] = None

  // description of how to make a filter rule
  def howToFilter: Option[String] = matchName map { name =>
    """ProblemFilters.exclude[%s]("%s")""".format(this.getClass.getSimpleName, name)
  }
}

trait TemplateRef extends ProblemRef {
  type Ref = ClassInfo
  def fileName: String = ref.sourceFileName
  def referredMember: String = ref.shortDescription
}

trait MemberRef extends ProblemRef {
  type Ref = MemberInfo
  def fileName: String = ref.owner.sourceFileName
  def referredMember: String = ref.fullName
}

sealed abstract class Problem extends ProblemRef {
  var affectedVersion = Problem.ClassVersion.New
  def description: String
}

abstract class TemplateProblem(override val ref: ClassInfo) extends Problem with TemplateRef {
  override def matchName = Some(ref.fullName)
}

abstract class MemberProblem(override val ref: MemberInfo) extends Problem with MemberRef {
  override def matchName = Some(referredMember)
}

case class MissingFieldProblem(oldfld: MemberInfo) extends MemberProblem(oldfld) {
  def description = oldfld.fieldString + " does not have a correspondent in " + affectedVersion + " version"
}

case class MissingMethodProblem(meth: MemberInfo) extends MemberProblem(meth) {
  def description = (if (meth.isDeferred && !meth.owner.isTrait) "abstract " else "") + meth.methodString + " does not have a correspondent in " + affectedVersion + " version"
}

case class UpdateForwarderBodyProblem(meth: MemberInfo) extends MemberProblem(meth) {
  assert(meth.owner.isTrait)
  assert(meth.owner.hasStaticImpl(meth))

  def description = "classes mixing " + meth.owner.fullName + " needs to update body of " + meth.shortMethodString
}

case class MissingClassProblem(oldclazz: ClassInfo) extends TemplateProblem(oldclazz) {
  def description = oldclazz.classString + " does not have a correspondent in " + affectedVersion + " version"
}

case class AbstractClassProblem(oldclazz: ClassInfo) extends TemplateProblem(oldclazz) {
  def description = oldclazz.classString + " was concrete; is declared abstract in " + affectedVersion + " version"
}

case class FinalClassProblem(oldclazz: ClassInfo) extends TemplateProblem(oldclazz) {
  def description = oldclazz.classString + " is declared final in " + affectedVersion + " version"
}

case class FinalMethodProblem(newmemb: MemberInfo) extends MemberProblem(newmemb) {
  def description = newmemb.methodString + " is declared final in " + affectedVersion + " version"
}

case class IncompatibleFieldTypeProblem(oldfld: MemberInfo, newfld: MemberInfo) extends MemberProblem(oldfld) {
  def description = newfld.fieldString + "'s type has changed; was: " + oldfld.tpe + ", is now: " + newfld.tpe
}

case class IncompatibleMethTypeProblem(oldmeth: MemberInfo, newmeths: List[MemberInfo]) extends MemberProblem(oldmeth) {
  def description = {
    oldmeth.methodString + (if (newmeths.tail.isEmpty)
      "'s type has changed; was " + oldmeth.tpe + ", is now: " + newmeths.head.tpe
    else
      " does not have a correspondent with same parameter signature among " +
        (newmeths map (_.tpe) mkString ", "))
  }
}

case class IncompatibleResultTypeProblem(oldmeth: MemberInfo, newmeth: MemberInfo) extends MemberProblem(oldmeth) {
  def description = {
    oldmeth.methodString + " has now a different result type; was: " +
      oldmeth.tpe.resultType + ", is now: " + newmeth.tpe.resultType
  }
}

case class AbstractMethodProblem(newmeth: MemberInfo) extends MemberProblem(newmeth) {
  def description = "abstract " + newmeth.methodString + " does not have a correspondent in " + affectedVersion + " version"
}

case class IncompatibleTemplateDefProblem(oldclazz: ClassInfo, newclazz: ClassInfo) extends TemplateProblem(oldclazz) {
  def description = {
    "declaration of " + oldclazz.description + " has changed to " + newclazz.description +
      " in new version; changing " + oldclazz.declarationPrefix + " to " + newclazz.declarationPrefix + " breaks client code"
  }
}

case class MissingTypesProblem(newclazz: ClassInfo, missing: Iterable[ClassInfo]) extends TemplateProblem(newclazz) {
  def description = "the type hierarchy of " + newclazz.description + " has changed in new version. " +
    "Missing types " + missing.map(_.fullName).mkString("{", ",", "}")
}

case class CyclicTypeReferenceProblem(clz: ClassInfo) extends TemplateProblem(clz) {
  def description = {
    "the type hierarchy of " + clz.description + " has changed in new version. Type " + clz.bytecodeName + " appears to be a subtype of itself"
  }
}

case class InaccessibleFieldProblem(newfld: MemberInfo) extends MemberProblem(newfld) {
  def description = newfld.fieldString + " was public; is inaccessible in " + affectedVersion + " version"
}

case class InaccessibleMethodProblem(newmeth: MemberInfo) extends MemberProblem(newmeth) {
  def description = newmeth.methodString + " was public; is inaccessible in " + affectedVersion + " version"
}

case class InaccessibleClassProblem(newclazz: ClassInfo) extends TemplateProblem(newclazz) {
  def description = newclazz.classString + " was public; is inaccessible in " + affectedVersion + " version"
}
