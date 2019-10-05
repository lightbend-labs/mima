package com.typesafe.tools.mima.lib.analyze

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.lib.analyze.field.FieldChecker
import com.typesafe.tools.mima.lib.analyze.method.MethodChecker
import com.typesafe.tools.mima.lib.analyze.template.TemplateChecker

object Analyzer {
  def analyze(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    assert(oldclazz.bytecodeName == newclazz.bytecodeName)

    if (oldclazz.isImplClass)
      return Nil // do not analyze trait's implementation classes

    TemplateChecker.check(oldclazz, newclazz) match {
      case Some(p @ (_: IncompatibleTemplateDefProblem | _: CyclicTypeReferenceProblem)) =>
        // these implies major incompatibility, does not make sense to continue
        List(p)

      case maybeProblem =>
        maybeProblem.toList :::
          FieldChecker.check(oldclazz, newclazz) :::
          MethodChecker.check(oldclazz, newclazz)
    }
  }
}
