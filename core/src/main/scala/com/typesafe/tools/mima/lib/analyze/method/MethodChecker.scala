package com.typesafe.tools.mima.lib.analyze.method

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.lib.analyze.Checker

private[analyze] abstract class BaseMethodChecker extends Checker[MethodInfo, ClassInfo] {
  import BaseMethodChecker._

  protected def check(oldmeth: MethodInfo, newclazz: ClassInfo, methsLookup: ClassInfo => Iterator[MethodInfo]): Option[Problem] = {
    val newmeths = methsLookup(newclazz).filter(oldmeth.paramsCount == _.paramsCount).toList
    newmeths.find(newmeth => hasMatchingDescriptorAndSignature(oldmeth, newmeth)) match {
      case Some(newmeth) => checkExisting1v1(oldmeth, newmeth)
      case None          =>
        val newmethAndBridges = newmeths.filter(oldmeth.matchesType(_)) // _the_ corresponding new method + its bridges
        newmethAndBridges.find(_.tpe.resultType == oldmeth.tpe.resultType) match {
          case Some(newmeth) => Some(IncompatibleSignatureProblem(oldmeth, newmeth))
          case None          =>
            if (newmeths.isEmpty || methsLookup(oldmeth.owner).toStream.lengthCompare(1) > 0) // method was overloaded
              Some(DirectMissingMethodProblem(oldmeth))
            else {
              newmethAndBridges match {
                case Nil          => Some(IncompatibleMethTypeProblem(oldmeth, uniques(newmeths)))
                case newmeth :: _ => Some(IncompatibleResultTypeProblem(oldmeth, newmeth))
              }
            }
        }
    }
  }

  private def checkExisting1v1(oldmeth: MethodInfo, newmeth: MethodInfo) = {
    if (newmeth.isLessVisibleThan(oldmeth))
      Some(InaccessibleMethodProblem(newmeth))
    else if (oldmeth.nonFinal && newmeth.isFinal && oldmeth.owner.nonFinal)
      Some(FinalMethodProblem(newmeth))
    else if (oldmeth.isConcrete && newmeth.isDeferred)
      Some(DirectAbstractMethodProblem(newmeth))
    else if (oldmeth.isStatic && !newmeth.isStatic)
      Some(StaticVirtualMemberProblem(oldmeth))
    else if (!oldmeth.isStatic && newmeth.isStatic)
      Some(VirtualStaticMemberProblem(oldmeth))
    else
      None
  }

  private def uniques(methods: Iterable[MethodInfo]): List[MethodInfo] =
    methods.groupBy(_.parametersDesc).values.map(_.head).toList
}
private[analyze] object BaseMethodChecker {
  def hasMatchingDescriptorAndSignature(oldMethod: MethodInfo, newMethod: MethodInfo): Boolean =
    oldMethod.descriptor == newMethod.descriptor && hasMatchingSignature(oldMethod.signature, newMethod.signature, newMethod.bytecodeName)

  // Assumes it is already checked that the descriptors match
  def hasMatchingSignature(oldSignature: String, newSignature: String, bytecodeName: String): Boolean =
  oldSignature == newSignature ||
    // Special case for https://github.com/scala/scala/pull/7975:
    (bytecodeName == MemberInfo.ConstructorName &&
      (newSignature.isEmpty ||
        // The dropped character is the leading '('
        oldSignature.endsWith(newSignature.tail)
      )
    )
}

private[analyze] class ClassMethodChecker extends BaseMethodChecker {
  def check(method: MethodInfo, inclazz: ClassInfo): Option[Problem] = {
    if (method.nonAccessible)
      None
    else if (method.isDeferred)
      super.check(method, inclazz, _.lookupMethods(method))
    else
      super.check(method, inclazz, _.lookupClassMethods(method))
  }
}

private[analyze] class TraitMethodChecker extends BaseMethodChecker {
  def check(method: MethodInfo, inclazz: ClassInfo): Option[Problem] = {
    if (method.nonAccessible)
      None
    else if (method.owner.hasStaticImpl(method))
      checkStaticImplMethod(method, inclazz)
    else
      super.check(method, inclazz, _.lookupMethods(method))
  }

  private def checkStaticImplMethod(method: MethodInfo, inclazz: ClassInfo) = {
    assert(method.owner.hasStaticImpl(method))
    if (inclazz.hasStaticImpl(method)) {
      // then it's ok, the method it is still there
      None
    } else {
      // if a concrete method exists on some inherited trait, then we
      // report the missing method but we can upgrade the bytecode for
      // this specific issue
      if (inclazz.allTraits.exists(_.hasStaticImpl(method))) {
        Some(UpdateForwarderBodyProblem(method))
      } else {
        // otherwise we check the all concrete trait methods and report
        // either that the method is missing or that no method with the
        // same signature exists. Either way, we expect that a problem is reported!
        val prob = super.check(method, inclazz, _.lookupConcreteTraitMethods(method))
        assert(prob.isDefined)
        prob
      }
    }
  }
}
