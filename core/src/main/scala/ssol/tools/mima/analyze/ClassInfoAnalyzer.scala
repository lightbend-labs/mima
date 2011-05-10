package ssol.tools.mima.analyze

import ssol.tools.mima.{ ClassInfo, Problem, IncompatibleTemplateDefProblem }

object ClassInfoAnalyzer {

  def apply(oldClazz: ClassInfo, newClazz: ClassInfo): List[Problem] = {
    val problems = TemplateDefCheck(oldClazz, newClazz).orElse(AbstractModifierCheck(oldClazz, newClazz)).orElse(FinalModifierCheck(oldClazz, newClazz)) match {
      case None => 
        if (oldClazz.isInterface) new TraitAnalyzer().analyze(oldClazz, newClazz)
        else new ClassAnalyzer().analyze(oldClazz, newClazz)

      case Some(p) => List(p)
    }

    problems.distinct
  }
}