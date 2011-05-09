package ssol.tools.mima.analyze

import ssol.tools.mima._

object TemplateDefCheck {
  def apply(thisClass: ClassInfo, thatClass: ClassInfo): Option[Problem] = {
    if (thisClass.isClass == thatClass.isClass) None
    else if (thisClass.isInterface == thatClass.isInterface) None // traits are handled as interfaces
    else Some(IncompatibleTemplateDefProblem(thisClass, thatClass))
  }
}
/*
object AbstractModifierCheck {
  def apply(thisClass: ClassInfo, thatClass: ClassInfo): Option[Problem] = {
    // a concrete class that is made abstract may entails binary incompatibility
    // because it can't be instantiated anymore
    if (!thisClass.isDeferred && thatClass.isDeferred) Some(AbstractClassProblem(thisClass))
    // note: Conversely, an abstract class that is made concrete entails no issue
    else None
  }

  def apply(thisMember: MemberInfo, thatMember: MemberInfo): Option[Problem] = {
    // A concrete member that is made abstract entail a binary incompatibilities because client
    // code may be calling it when no concrete implementation exists
    if(!thisMember.isDeferred && thatMember.isDeferred) Some(problem)
    // note: Conversely, an abstract member that is made concrete does not entail incompatibilities 
    // because no client code relied on it.
    else None
  }
}
*/

trait AccessModifierCheck[T <: WithAccessModifier] {
  def apply(thisElement: T, thatElement: T): Option[Problem] = {
    if (thatElement.isLessVisibleThan(thisElement)) Some(createInaccessibleProblem(thisElement, thatElement))
    else None
  }

  def createInaccessibleProblem(thisElement: T, thatElement: T): Problem
}

object InaccessibleMemberCheck extends AccessModifierCheck[MemberInfo] {
  def createInaccessibleProblem(thisElement: MemberInfo, thatElement: MemberInfo): Problem = {
    assert(thisElement.isMethod == thatElement.isMethod)
    if (thisElement.isMethod) InaccessibleMethodProblem(thatElement) else InaccessibleFieldProblem(thatElement)
  }
}

object InaccessibleClassCheck extends AccessModifierCheck[ClassInfo] {
  def createInaccessibleProblem(thisElement: ClassInfo, thatElement: ClassInfo): Problem =
    InaccessibleClassProblem(thatElement)
}

object MethodCheck {
  def apply(m: MemberInfo, clazz: ClassInfo): Option[Problem] = {
    assert(m.isMethod)
    assert(m.owner.isClass == clazz.isClass)
    assert(!clazz.isImplClass)

    if (m.owner.isTrait) checkTraitMethod(m, clazz)
    else checkClassMethod(m, clazz)
  }

  private def checkClassMethod(m: MemberInfo, clazz: ClassInfo) =
    if (m.isDeferred) check(m, clazz.lookupMethods(m.name)) else check(m, clazz.lookupClassMethods(m.name))

  private def checkTraitMethod(m: MemberInfo, clazz: ClassInfo) = {
    if (clazz.hasStaticImpl(m)) {
      // then it's ok, the method it is still there
      None
    } // if a concrete method exists on some inherited trait, then we report the missing method 
    // but we can upgrade the bytecode for this specific issue
    else {
      if (clazz.allTraits.exists(_.hasStaticImpl(m))) {
        Some(UpdateForwarderBodyProblem(m))
      } else {
        val allConcreteMeths = clazz.lookupConcreteTraitMethods(m.name).toList
        val res = check(m, allConcreteMeths)
        assert(res.isDefined)
        res
      }
    }
  }

  private def check(m: MemberInfo, in: Iterator[MemberInfo]): Option[Problem] = check(m, in.toList)
  private def check(m: MemberInfo, in: List[MemberInfo]): Option[Problem] = {
    val meths = in filter (m.params.size == _.params.size)
    if (meths.isEmpty)
      Some(MissingMethodProblem(m))
    else {
      meths find (_.sig == m.sig) match {
        case None =>
          meths find (m matchesType _) match {
            case None =>
              Some(IncompatibleMethTypeProblem(m, uniques(meths)))
            case Some(found) =>
              Some(IncompatibleResultTypeProblem(m, found))
          }

        case Some(found) if (found.isLessVisibleThan(m)) =>
          Some(InaccessibleMethodProblem(found))

        case Some(found) if (!m.isDeferred && found.isDeferred) =>
          Some(AbstractMethodProblem(found))

        case _ => None
      }
    }
  }

  private def uniques(methods: List[MemberInfo]): List[MemberInfo] =
    methods.groupBy(_.parametersSig).values.map(_.head).toList
}