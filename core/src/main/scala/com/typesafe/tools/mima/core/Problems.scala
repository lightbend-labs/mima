package com.typesafe.tools.mima.core

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
  // each description accepts a name for the affected files,
  // and generates the corresponding diagnostic message.
  // For backward checking, the affected version is "current",
  // while for forward checking it could be "other" or "previous",
  // for example.
  def description: String => String
}

abstract class TemplateProblem(override val ref: ClassInfo) extends Problem with TemplateRef {
  override def matchName = Some(ref.fullName)
}

abstract class MemberProblem(override val ref: MemberInfo) extends Problem with MemberRef {
  override def matchName = Some(referredMember)
}

case class MissingFieldProblem(oldfld: MemberInfo) extends MemberProblem(oldfld) {
  def description = affectedVersion => oldfld.fieldString + " does not have a correspondent in " + affectedVersion + " version"
}

abstract class MissingMethodProblem(meth: MemberInfo) extends MemberProblem(meth)

case class DirectMissingMethodProblem(meth: MemberInfo) extends MissingMethodProblem(meth) {
  def description = affectedVersion => (if (meth.isDeferred && !meth.owner.isTrait) "abstract " else "") + meth.methodString + " does not have a correspondent in " + affectedVersion + " version"
}

case class ReversedMissingMethodProblem(meth: MemberInfo) extends MissingMethodProblem(meth) {
  def description = affectedVersion => (if (meth.isDeferred && !meth.owner.isTrait) "abstract " else "") + meth.methodString + " is present only in " + affectedVersion + " version"
}

case class UpdateForwarderBodyProblem(meth: MemberInfo) extends MemberProblem(meth) {
  assert(meth.owner.isTrait)
  assert(meth.owner.hasStaticImpl(meth))

  def description = affectedVersion => "in " + affectedVersion + " version, classes mixing " + meth.owner.fullName + " needs to update body of " + meth.shortMethodString
}

case class MissingClassProblem(oldclazz: ClassInfo) extends TemplateProblem(oldclazz) {
  def description = affectedVersion => oldclazz.classString + " does not have a correspondent in " + affectedVersion + " version"
}

case class AbstractClassProblem(oldclazz: ClassInfo) extends TemplateProblem(oldclazz) {
  def description = affectedVersion => oldclazz.classString + " was concrete; is declared abstract in " + affectedVersion + " version"
}

case class FinalClassProblem(oldclazz: ClassInfo) extends TemplateProblem(oldclazz) {
  def description = affectedVersion => oldclazz.classString + " is declared final in " + affectedVersion + " version"
}

case class FinalMethodProblem(newmemb: MemberInfo) extends MemberProblem(newmemb) {
  def description = affectedVersion => newmemb.methodString + " is declared final in " + affectedVersion + " version"
}

case class IncompatibleFieldTypeProblem(oldfld: MemberInfo, newfld: MemberInfo) extends MemberProblem(oldfld) {
  def description = affectedVersion => newfld.fieldString + "'s type is different in " + affectedVersion + " version, where it is: " + newfld.tpe + " rather than: " + oldfld.tpe
}

case class IncompatibleMethTypeProblem(oldmeth: MemberInfo, newmeths: List[MemberInfo]) extends MemberProblem(oldmeth) {
  def description = affectedVersion => {
    oldmeth.methodString + (if (newmeths.tail.isEmpty)
      "'s type is different in " + affectedVersion + " version, where it is " + newmeths.head.tpe + " instead of " + oldmeth.tpe
    else
      " in " + affectedVersion + " version does not have a correspondent with same parameter signature among " +
        (newmeths map (_.tpe) mkString ", "))
  }
}

case class IncompatibleResultTypeProblem(oldmeth: MemberInfo, newmeth: MemberInfo) extends MemberProblem(oldmeth) {
  def description = affectedVersion => {
    oldmeth.methodString + " has a different result type in " + affectedVersion + " version, where it is " + newmeth.tpe.resultType +
       " rather than " + oldmeth.tpe.resultType
  }
}

// In some older code within Mima, the affectedVersion could be reversed. We split AbstractMethodProblem and MissingMethodProblem
// into two, in case the affected version is the other one, rather than the current one. (reversed if forward check).
abstract class AbstractMethodProblem(newmeth: MemberInfo) extends MemberProblem(newmeth)

case class InheritedNewAbstractMethodProblem(clazz: ClassInfo, inheritedMethod: MemberInfo) extends AbstractMethodProblem(inheritedMethod) {
  def description = affectedVersion => "abstract " + inheritedMethod.methodString+ " is inherited by class " + clazz.bytecodeName + " in " + affectedVersion + " version."
}

case class DirectAbstractMethodProblem(newmeth: MemberInfo) extends AbstractMethodProblem(newmeth) {
  def description = affectedVersion => "abstract " + newmeth.methodString + " does not have a correspondent in " + affectedVersion + " version"
}

case class StaticVirtualMemberProblem(newmeth: MemberInfo) extends AbstractMethodProblem(newmeth) {
  def description = affectedVersion => "non-static " + newmeth.memberString + " is static in " + affectedVersion + " version"
}
case class VirtualStaticMemberProblem(newmeth: MemberInfo) extends AbstractMethodProblem(newmeth) {
  def description = affectedVersion => "static " + newmeth.memberString + " is non-static in " + affectedVersion + " version"
}

case class ReversedAbstractMethodProblem(newmeth: MemberInfo) extends MemberProblem(newmeth) {
  def description = affectedVersion => "in " + affectedVersion + " version there is abstract " + newmeth.methodString + ", which does not have a correspondent"
}

case class IncompatibleTemplateDefProblem(oldclazz: ClassInfo, newclazz: ClassInfo) extends TemplateProblem(oldclazz) {
  def description = affectedVersion => {
    "declaration of " + oldclazz.description + " is " + newclazz.description +
      " in " + affectedVersion + " version; changing " + oldclazz.declarationPrefix + " to " + newclazz.declarationPrefix + " breaks client code"
  }
}

case class MissingTypesProblem(newclazz: ClassInfo, missing: Iterable[ClassInfo]) extends TemplateProblem(newclazz) {
  def description = affectedVersion => "the type hierarchy of " + newclazz.description + " is different in " + affectedVersion + " version. " +
    "Missing types " + missing.map(_.fullName).mkString("{", ",", "}")
}

case class CyclicTypeReferenceProblem(clz: ClassInfo) extends TemplateProblem(clz) {
  def description = affectedVersion => {
    "the type hierarchy of " + clz.description + " is different in " + affectedVersion + " version. Type " + clz.bytecodeName + " appears to be a subtype of itself"
  }
}

case class InaccessibleFieldProblem(newfld: MemberInfo) extends MemberProblem(newfld) {
  def description = affectedVersion => newfld.fieldString + " is inaccessible in " + affectedVersion + " version, it must be public."
}

case class InaccessibleMethodProblem(newmeth: MemberInfo) extends MemberProblem(newmeth) {
  def description = affectedVersion => newmeth.methodString + " is inaccessible in " + affectedVersion + " version, it must be public."
}

case class InaccessibleClassProblem(newclazz: ClassInfo) extends TemplateProblem(newclazz) {
  def description = affectedVersion => newclazz.classString + " is inaccessible in " + affectedVersion + " version, it must be public."
}
