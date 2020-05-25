package com.typesafe.tools.mima.lib.analyze.method

import scala.annotation.tailrec

import com.typesafe.tools.mima.core._

private[analyze] object MethodChecker {
  def check(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    checkExisting(oldclazz, newclazz) ::: checkNew(oldclazz, newclazz)

  /** Analyze incompatibilities that may derive from changes in existing methods. */
  private def checkExisting(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    for (oldmeth <- oldclazz.methods.value; problem <- checkExisting1(oldmeth, newclazz)) yield problem
  }

  /** Analyze incompatibilities that may derive from new methods in `newclazz`. */
  private def checkNew(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    (if (newclazz.isClass) Nil else checkEmulatedConcreteMethodsProblems(oldclazz, newclazz)) :::
      checkDeferredMethodsProblems(oldclazz, newclazz) :::
      checkInheritedNewAbstractMethodProblems(oldclazz, newclazz)
  }

  private def checkExisting1(oldmeth: MethodInfo, newclazz: ClassInfo): Option[Problem] = {
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

  def hasMatchingSignature(oldsig: String, newsig: String, bytecodeName: String): Boolean = {
    def canonicalize(signature: String): String = {
      signature.headOption match {
        case None | Some('(') => signature
        case _ =>
            val (formalTypeParameters, rest) = FormalTypeParameter.parseList(signature.drop(1))
            val replacements = formalTypeParameters.map(_.identifier).zipWithIndex
            replacements.foldLeft(signature) { case (sig, (from, to)) =>
              sig
                .replace(s"<${from}:", s"<__${to}__:")
                .replace(s";${from}:", s";__${to}__:")
                .replace(s"T${from};", s"__${to}__") }
      }
    }

    oldsig == newsig ||
      canonicalize(oldsig) == canonicalize(newsig) || // Special case for scala#7975
      bytecodeName == MemberInfo.ConstructorName && hasMatchingCtorSig(oldsig, newsig)
  }

  case class FormalTypeParameter(identifier: String, bound: String)
  object FormalTypeParameter {
    def parseList(in: String, listSoFar: List[FormalTypeParameter] = Nil): (List[FormalTypeParameter], String) = {
      in(0) match {
        case '>' => (listSoFar, in.drop(1))
        case o => {
          val (next, rest) = parseOne(in)
          parseList(rest, listSoFar :+ next)
        }
      }
    }
    def parseOne(in: String): (FormalTypeParameter, String) = {
      val identifier = in.takeWhile(_ != ':')
      val boundAndRest = in.dropWhile(_ != ':').drop(1)
      val (bound, rest) = splitBoundAndRest(boundAndRest)
      (FormalTypeParameter(identifier, bound), rest)
    }
    @tailrec
    private def splitBoundAndRest(in: String, boundSoFar: String = "", depth: Int = 0): (String, String) = {
      if (depth > 0) {
        in(0) match {
          case '>' => splitBoundAndRest(in.drop(1), boundSoFar + '>', depth - 1)
          case '<' => splitBoundAndRest(in.drop(1), boundSoFar + '<', depth + 1)
          case o => splitBoundAndRest(in.drop(1), boundSoFar + o, depth)
        }
      } else {
        in(0) match {
          case '<' => splitBoundAndRest(in.drop(1), boundSoFar + '<', depth + 1)
          case ';' => (boundSoFar, in.drop(1))
          case o => splitBoundAndRest(in.drop(1), boundSoFar + o, depth)
        }
      }
    }
  }

  private def hasMatchingCtorSig(oldsig: String, newsig: String): Boolean =
    newsig.isEmpty ||              // ignore losing signature on constructors
      oldsig.endsWith(newsig.tail) // ignore losing the 1st (outer) param (.tail drops the leading '(')

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
        val oldmethsDescriptors = methsLookup(oldmeth.owner).map(_.descriptor).toSet
        if (newmeths.forall(newmeth => oldmethsDescriptors.contains(newmeth.descriptor)))
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

  private def checkEmulatedConcreteMethodsProblems(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    for {
      newmeth <- newclazz.emulatedConcreteMethods.iterator
      if !oldclazz.hasStaticImpl(newmeth)
      problem <- {
        if (oldclazz.lookupMethods(newmeth).exists(_.descriptor == newmeth.descriptor)) {
          // a static implementation for the same method existed already, therefore
          // classes that mixed-in the trait already have a forwarder to the implementation
          // class. Mind that, despite no binary incompatibility arises, program's
          // semantic may be severely affected.
          None
        } else {
          // this means that the method is brand new
          // and therefore the implementation has to be injected
          Some(ReversedMissingMethodProblem(newmeth))
        }
      }
    } yield problem
  }.toList

  private def checkDeferredMethodsProblems(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    for {
      newmeth <- newclazz.deferredMethods.iterator
      problem <- oldclazz.lookupMethods(newmeth).find(_.descriptor == newmeth.descriptor) match {
        case None          => Some(ReversedMissingMethodProblem(newmeth))
        case Some(oldmeth) =>
          if (newclazz.isClass && oldmeth.isConcrete)
            Some(ReversedAbstractMethodProblem(newmeth))
          else None
      }
    } yield problem
  }.toList

  private def checkInheritedNewAbstractMethodProblems(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    def allInheritedTypes(clazz: ClassInfo) = clazz.superClasses ++ clazz.allInterfaces
    val diffInheritedTypes = allInheritedTypes(newclazz).diff(allInheritedTypes(oldclazz))

    def noInheritedMatchingMethod(clazz: ClassInfo, meth: MethodInfo)(p: MemberInfo => Boolean) = {
      !clazz.lookupMethods(meth).filter(_.matchesType(meth)).exists(m => m.owner != meth.owner && p(m))
    }

    for {
      newInheritedType <- diffInheritedTypes.iterator
      // if `newInheritedType` is a trait, then the trait's concrete methods should be counted as deferred methods
      newDeferredMethod <- newInheritedType.deferredMethodsInBytecode
      // checks that the newDeferredMethod did not already exist in one of the oldclazz supertypes
      if noInheritedMatchingMethod(oldclazz, newDeferredMethod)(_ => true) &&
          // checks that no concrete implementation of the newDeferredMethod is provided by one of the newclazz supertypes
          noInheritedMatchingMethod(newclazz, newDeferredMethod)(_.isConcrete)
    } yield {
      // report a binary incompatibility as there is a new inherited abstract method, which can lead to a AbstractErrorMethod at runtime
      val newmeth = new MethodInfo(newclazz, newDeferredMethod.bytecodeName, newDeferredMethod.flags, newDeferredMethod.descriptor)
      InheritedNewAbstractMethodProblem(newDeferredMethod, newmeth)
    }
  }.toList
}
