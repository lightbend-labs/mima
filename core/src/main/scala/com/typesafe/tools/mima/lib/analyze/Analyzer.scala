package com.typesafe.tools.mima.lib.analyze

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.core.util.log.Logging
import com.typesafe.tools.mima.lib.analyze.field.FieldChecker
import com.typesafe.tools.mima.lib.analyze.method.MethodChecker
import com.typesafe.tools.mima.lib.analyze.template.TemplateChecker

object Analyzer {
  def analyze(oldpkg: PackageInfo, newpkg: PackageInfo, log: Logging, excludeAnnots: List[AnnotInfo]): List[Problem] = {
    for {
      oldclazz <- oldpkg.accessibleClasses.toList.sortBy(_.bytecodeName)
      _ = log.verbose(s"analyzing $oldclazz")
      _ = oldclazz.forceLoad
      // if it is missing a trait implementation class, then no error should be reported
      // since there should be already errors, i.e., missing methods...
      if !oldclazz.isImplClass
      if !(if (oldclazz.isModuleClass) oldclazz.module else oldclazz).isScopedPrivate
      if !excludeAnnots.exists(oldclazz.annotations.contains)
      problem <- newpkg.classes.get(oldclazz.bytecodeName) match {
        case Some(newclazz) => analyze(oldclazz, newclazz, log, excludeAnnots)
        case None           => List(MissingClassProblem(oldclazz))
      }
    } yield {
      log.debug(s"problem found: ${problem.description("new")}")
      problem
    }
  }

  def analyze(oldclazz: ClassInfo, newclazz: ClassInfo, log: Logging, excludeAnnots: List[AnnotInfo]): List[Problem] = {
    TemplateChecker.check(oldclazz, newclazz) match {
      case p @ Some(_: IncompatibleTemplateDefProblem | _: CyclicTypeReferenceProblem) =>
        // these implies major incompatibility, does not make sense to continue
        p.toList

      case maybeProblem =>
        maybeProblem.toList :::
          FieldChecker.check(oldclazz, newclazz) :::
          MethodChecker.check(oldclazz, newclazz, excludeAnnots)
    }
  }
}
