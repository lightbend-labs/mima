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

    val templateProblems = analyzeTemplateDecl(oldclazz, newclazz)

    if (templateProblems.exists(p => p.isInstanceOf[IncompatibleTemplateDefProblem] ||
      p.isInstanceOf[CyclicTypeReferenceProblem]))
      templateProblems // IncompatibleTemplateDefProblem implies major incompatibility, does not make sense to continue
    else
      templateProblems ::: analyzeMembers(oldclazz, newclazz)
  }

  def analyzeTemplateDecl(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    TemplateChecker.check(oldclazz, newclazz).toList

  def analyzeMembers(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    FieldChecker.check(oldclazz, newclazz) ::: MethodChecker.check(oldclazz, newclazz)
}
