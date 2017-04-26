package com.typesafe.tools.mima.lib.analyze.method

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima._
import com.typesafe.tools.mima.lib.analyze.Checker

private[analyze] abstract class BaseMethodChecker extends Checker[MemberInfo, ClassInfo] {
  import MethodRules._

  protected val rules = Seq(AccessModifier, FinalModifier, AbstractModifier, JavaStatic)

  protected def check(method: MemberInfo, in: TraversableOnce[MemberInfo]): Option[Problem] = {
    val meths = (in filter (method.params.size == _.params.size)).toList
    if (meths.isEmpty)
      Some(DirectMissingMethodProblem(method))
    else {
      meths find (_.sig == method.sig) match {
        case None =>
          meths find (method matchesType _) match {
            case None =>
              Some(IncompatibleMethTypeProblem(method, uniques(meths)))
            case Some(found) =>
              Some(IncompatibleResultTypeProblem(method, found))
          }

        case Some(found) =>
          checkRules(rules)(method, found)

        case _ => None
      }
    }
  }

  private def uniques(methods: List[MemberInfo]): List[MemberInfo] =
    methods.groupBy(_.parametersSig).values.map(_.head).toList
}

private[analyze] class ClassMethodChecker extends BaseMethodChecker {
  def check(method: MemberInfo, inclazz: ClassInfo): Option[Problem] = {
    if (method.nonAccessible)
      None
    else if (method.isDeferred)
      super.check(method, inclazz.lookupMethods(method.bytecodeName))
    else
      super.check(method, inclazz.lookupClassMethods(method.bytecodeName))
  }
}

private[analyze] class TraitMethodChecker extends BaseMethodChecker {
  def check(method: MemberInfo, inclazz: ClassInfo): Option[Problem] = {
    if (method.nonAccessible)
      None
    else if (method.owner.hasStaticImpl(method))
      checkStaticImplMethod(method, inclazz)
    else
      super.check(method, inclazz.lookupMethods(method.bytecodeName))
  }

  private def checkStaticImplMethod(method: MemberInfo, inclazz: ClassInfo) = {
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
        val prob = super.check(method, inclazz.lookupConcreteTraitMethods(method.bytecodeName))
        assert(prob.isDefined)
        prob
      }
    }
  }
}
