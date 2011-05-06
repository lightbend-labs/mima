package ssol.tools.mima.analyze

import ssol.tools.mima._

object MethodAnalyzer {

  private object MethodProblemBuilder {
    //FIXME[mirco]: We need to watch for flags. A public method that becomes private is an issue,
    //  while a public method that becomes a @bridge public method is ok! This need to be implemented!
    def apply(version: Problem.ClassVersion.Value)(m: MemberInfo, foundmeths: List[MemberInfo]): Option[Problem] = {
      val meths = foundmeths filter (m.params.size == _.params.size)
      if (meths.nonEmpty) {
        meths find (_.sig == m.sig) match {
          case None =>
            meths find (m matchesType _) match {
              case None =>
                Some(IncompatibleMethTypeProblem(m, uniques(meths)))
              case Some(found) =>
                Some(IncompatibleResultTypeProblem(m, found))
            }

          case Some(found) if (!found.isPublic) =>
            Some(InaccessibleMethodProblem(found))

          case Some(found) if (!m.isDeferred && found.isDeferred) =>
            Some(AbstractMethodProblem(found))

          case _ => None
        }
      } else Some(MissingMethodProblem(m)(version))
    }

    private def uniques(methods: List[MemberInfo]): List[MemberInfo] =
      methods.groupBy(_.parametersSig).values.map(_.head).toList
  }

  object AnalyzeClassMethod {
    def apply(version: Problem.ClassVersion.Value)(m: MemberInfo, clazz: ClassInfo): Option[Problem] = {
      assert(m.isMethod)

      if (m.isDeferred) {
        AnalyzeDeferredMethod(version)(m, clazz)
      } else {
        val foundmeths = clazz.lookupClassMethods(m.name)
        MethodProblemBuilder(version)(m, foundmeths.toList)
      }
    }
  }

  object AnalyzeTraitImplMethod {
    def apply(version: Problem.ClassVersion.Value)(m: MemberInfo, clazz: ClassInfo): Option[Problem] = {
      assert(m.isMethod)
      assert(!m.owner.isClass)
      assert(!clazz.isClass)
      assert(m.isDeferred, m.fullName)

      val same = (mold: MemberInfo, mnew: MemberInfo) => mold.name == mnew.name && mold.sig == mnew.sig

      if (clazz.hasStaticImpl(m)) {
        // then it's ok, the method it is still there
        None
      } // if a concrete method exists on some inherited trait, then we report the missing method 
      // but we can upgrade the bytecode for this specific issue
      else {
        if (clazz.allTraits.toList.exists(_.hasStaticImpl(m))) {
          Some(UpdateForwarderBodyProblem(m))
        } else {
          val allConcreteMeths = clazz.allTraits.toList.flatten(_.concreteMethods).filter(_.name == m.name)
          MethodProblemBuilder(version)(m, allConcreteMeths) match {
            case None   => assert(false); None

            case others => others
          }
        }
      }
    }
  }

  object AnalyzeDeferredMethod {
    def apply(version: Problem.ClassVersion.Value)(m: MemberInfo, clazz: ClassInfo): Option[Problem] = {
      assert(m.isMethod)
      assert(m.isDeferred)

      val foundmeths = clazz.lookupMethods(m.name)

      MethodProblemBuilder(version)(m, foundmeths.toList)
    }
  }
}