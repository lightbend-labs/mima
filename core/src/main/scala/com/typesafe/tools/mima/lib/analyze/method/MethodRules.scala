package com.typesafe.tools.mima.lib.analyze.method

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.lib.analyze.Rule

private[method] object MethodRules {
  trait MethodRule extends Rule[MethodInfo, MethodInfo]

  object FinalModifier extends MethodRule {
    def apply(thisMember: MethodInfo, thatMember: MethodInfo): Option[Problem] = {
      // A non-final member that is made final entails a binary incompatibilities because client
      // code may be overriding it
      if (!thisMember.owner.isFinal && thisMember.nonFinal && thatMember.isFinal) Some(FinalMethodProblem(thatMember))
      // note: Conversely, a final member that is made non-final does not entail incompatibilities
      else None
    }
  }

  object AccessModifier extends MethodRule {
    def apply(thisMember: MethodInfo, thatMember: MethodInfo): Option[Problem] = {
      if (thatMember.isLessVisibleThan(thisMember)) Some(InaccessibleMethodProblem(thatMember))
      else None
    }
  }

  object AbstractModifier extends MethodRule {
    def apply(thisMember: MethodInfo, thatMember: MethodInfo): Option[Problem] = {
      // A concrete member that is made abstract entail a binary incompatibilities because client
      // code may be calling it when no concrete implementation exists
      if (thisMember.isConcrete && thatMember.isDeferred) Some(DirectAbstractMethodProblem(thatMember))
      // note: Conversely, an abstract member that is made concrete does not entail incompatibilities
      // because no client code relied on it.
      else None
    }
  }

  object JavaStatic extends MethodRule {
    def apply(thisMember: MethodInfo, thatMember: MethodInfo): Option[Problem] = {
      if (thisMember.isStatic && !thatMember.isStatic) Some(StaticVirtualMemberProblem(thisMember))
      else if (!thisMember.isStatic && thatMember.isStatic) Some(VirtualStaticMemberProblem(thisMember))
      else None
    }
  }
}
