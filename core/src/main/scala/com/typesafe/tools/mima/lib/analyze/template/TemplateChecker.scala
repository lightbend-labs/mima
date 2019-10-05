package com.typesafe.tools.mima.lib.analyze.template

import com.typesafe.tools.mima.core._

private[analyze] object TemplateChecker {
  def check(oldclazz: ClassInfo, newclazz: ClassInfo): Option[Problem] = {
    if (oldclazz.isInterface != newclazz.isInterface)
      Some(IncompatibleTemplateDefProblem(oldclazz, newclazz))
    else if (newclazz.isLessVisibleThan(oldclazz))
      Some(InaccessibleClassProblem(newclazz))
    else if (oldclazz.isConcrete && newclazz.isDeferred)
      Some(AbstractClassProblem(oldclazz))
    else if (oldclazz.nonFinal && newclazz.isFinal)
      Some(FinalClassProblem(oldclazz))
    else if (newclazz.superClasses.contains(newclazz))
      Some(CyclicTypeReferenceProblem(newclazz))
    else {
      val missingSuperClasses = oldclazz.superClasses.diff(newclazz.superClasses)
      val missingInterfaces = oldclazz.allInterfaces.diff(newclazz.allInterfaces)
      if (missingSuperClasses.nonEmpty)
        Some(MissingTypesProblem(newclazz, missingSuperClasses))
      else if (missingInterfaces.nonEmpty)
        Some(MissingTypesProblem(newclazz, missingInterfaces))
      else
        None
    }
  }
}
