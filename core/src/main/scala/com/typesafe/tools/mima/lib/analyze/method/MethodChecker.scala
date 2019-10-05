package com.typesafe.tools.mima.lib.analyze.method

import com.typesafe.tools.mima.core._

private[analyze] object MethodChecker {
  /** Analyze incompatibilities that may derive from methods in the `oldclazz`. */
  def check(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    for (oldfld <- oldclazz.methods.value; problem <- check1(oldfld, newclazz)) yield problem
  }

  private def check1(oldmeth: MethodInfo, newclazz: ClassInfo): Option[Problem] = {
    if (oldmeth.nonAccessible)
      None
    else if (newclazz.isClass) {
      if (oldmeth.isDeferred)
        checkExisting1Impl(oldmeth, newclazz, _.lookupMethods(oldmeth))
      else
        checkExisting1Impl(oldmeth, newclazz, _.lookupClassMethods(oldmeth))
    } else {
      if (oldmeth.owner.hasStaticImpl(oldmeth))
        checkStaticImplMethod(oldmeth, newclazz)
      else
        checkExisting1Impl(oldmeth, newclazz, _.lookupMethods(oldmeth))
    }
  }

  private def checkExisting1Impl(oldmeth: MethodInfo, newclazz: ClassInfo, methsLookup: ClassInfo => Iterator[MethodInfo]): Option[Problem] = {
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

  private def hasMatchingDescriptorAndSignature(oldmeth: MethodInfo, newmeth: MethodInfo): Boolean = {
    oldmeth.descriptor == newmeth.descriptor &&
        hasMatchingSignature(oldmeth.signature, newmeth.signature, newmeth.bytecodeName)
  }

  private[analyze] def hasMatchingSignature(oldsig: String, newsig: String, bytecodeName: String): Boolean = {
    oldsig == newsig || {
      // Special case for https://github.com/scala/scala/pull/7975:
      // uses .tail to drop the leading '(' in the signature
      bytecodeName == MemberInfo.ConstructorName && (newsig.isEmpty || oldsig.endsWith(newsig.tail))
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
        val prob = check(method, inclazz, _.lookupConcreteTraitMethods(method))
        assert(prob.isDefined)
        prob
      }
    }
  }
}
