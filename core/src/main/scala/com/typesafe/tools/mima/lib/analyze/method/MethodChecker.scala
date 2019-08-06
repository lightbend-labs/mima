package com.typesafe.tools.mima.lib.analyze.method

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.lib.analyze.Checker

private[analyze] abstract class BaseMethodChecker extends Checker[MethodInfo, ClassInfo] {
  import MethodRules._

  protected val rules = Seq(AccessModifier, FinalModifier, AbstractModifier, JavaStatic)

  protected def check(method: MethodInfo, newclazz: ClassInfo, methsLookup: ClassInfo => Iterator[MethodInfo]): Option[Problem] = {
    val meths = methsLookup(newclazz).filter(method.paramsCount == _.paramsCount).toList // newmeths
    if (meths.isEmpty)
      Some(DirectMissingMethodProblem(method))
    else {
      meths.find(m => m.descriptor == method.descriptor && methSigCheck(method, m)) match {
        case Some(found) => checkRules(rules)(method, found)
        case None        =>
          val filtered = meths.filter(method.matchesType(_))
          filtered.find(_.tpe.resultType == method.tpe.resultType) match {
            case Some(found) => Some(IncompatibleSignatureProblem(method, found))
            case None        =>
              val oldmethsDescriptors = methsLookup(method.owner).map(_.descriptor).toSet
              if (meths.forall(newmeth => oldmethsDescriptors.contains(newmeth.descriptor)))
                Some(DirectMissingMethodProblem(method))
              else {
                filtered match {
                  case Nil        => Some(IncompatibleMethTypeProblem(method, uniques(meths)))
                  case first :: _ => Some(IncompatibleResultTypeProblem(method, first))
                }
              }
          }
      }
    }
  }

  private def methSigCheck(oldmeth: MethodInfo, newMeth: MethodInfo): Boolean = {
    oldmeth.signature == newMeth.signature || (
      newMeth.bytecodeName == MemberInfo.ConstructorName && newMeth.signature.isEmpty
    )
  }

  private def uniques(methods: List[MethodInfo]): List[MethodInfo] =
    methods.groupBy(_.parametersDesc).values.map(_.head).toList
}

private[analyze] class ClassMethodChecker extends BaseMethodChecker {
  def check(method: MethodInfo, inclazz: ClassInfo): Option[Problem] = {
    if (method.nonAccessible)
      None
    else if (method.isDeferred)
      super.check(method, inclazz, _.lookupMethods(method.bytecodeName))
    else
      super.check(method, inclazz, _.lookupClassMethods(method.bytecodeName))
  }
}

private[analyze] class TraitMethodChecker extends BaseMethodChecker {
  def check(method: MethodInfo, inclazz: ClassInfo): Option[Problem] = {
    if (method.nonAccessible)
      None
    else if (method.owner.hasStaticImpl(method))
      checkStaticImplMethod(method, inclazz)
    else
      super.check(method, inclazz, _.lookupMethods(method.bytecodeName))
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
        val prob = super.check(method, inclazz, _.lookupConcreteTraitMethods(method.bytecodeName))
        assert(prob.isDefined)
        prob
      }
    }
  }
}
