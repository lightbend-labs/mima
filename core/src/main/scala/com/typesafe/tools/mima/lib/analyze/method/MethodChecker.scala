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
      case None          => Some(missingOrIncompatible(oldmeth, newmeths, methsLookup))
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

  private def checkStaticImplMethod(oldmeth: MethodInfo, newclazz: ClassInfo) = {
    if (newclazz.hasStaticImpl(oldmeth)) {
      None // then it's ok, the method it is still there
    } else {
      // if a concrete method exists on some inherited trait, then we
      // report the missing method but we can upgrade the bytecode for
      // this specific issue
      if (newclazz.allTraits.exists(_.hasStaticImpl(oldmeth))) {
        Some(UpdateForwarderBodyProblem(oldmeth))
      } else {
        // otherwise we check all the concrete trait methods and report
        // the missing or incompatible method.
        val methsLookup = (_: ClassInfo).lookupConcreteTraitMethods(oldmeth)
        Some(missingOrIncompatible(oldmeth, methsLookup(newclazz).toList, methsLookup))
      }
    }
  }

  private def missingOrIncompatible(oldmeth: MethodInfo, newmeths: List[MethodInfo], methsLookup: ClassInfo => Iterator[MethodInfo]) = {
    val newmethAndBridges = newmeths.filter(oldmeth.matchesType(_)) // _the_ corresponding new method + its bridges
    newmethAndBridges.find(_.tpe.resultType == oldmeth.tpe.resultType) match {
      case Some(newmeth) => IncompatibleSignatureProblem(oldmeth, newmeth)
      case None          =>
        if (newmeths.isEmpty || methsLookup(oldmeth.owner).toStream.lengthCompare(1) > 0) // method was overloaded
          DirectMissingMethodProblem(oldmeth)
        else {
          newmethAndBridges match {
            case Nil          => IncompatibleMethTypeProblem(oldmeth, uniques(newmeths))
            case newmeth :: _ => IncompatibleResultTypeProblem(oldmeth, newmeth)
          }
        }
    }
  }

  private def uniques(methods: Iterable[MethodInfo]): List[MethodInfo] =
    methods.groupBy(_.parametersDesc).values.collect { case method :: _ => method }.toList
}
