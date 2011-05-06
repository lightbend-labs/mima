package ssol.tools.mima.analyze

import ssol.tools.mima._
import MethodAnalyzer._

private class TraitAnalyzer(oldClazz: ClassInfo, newClazz: ClassInfo) extends BaseClassAnalyzer(oldClazz, newClazz) {

  override protected def checkOldMethods() {
    checkConcreteMethods(Problem.ClassVersion.New, oldClazz, newClazz)
    checkDeferredMethods(Problem.ClassVersion.New, oldClazz, newClazz)
  }

  private def checkConcreteMethods(classVersion: Problem.ClassVersion.Value, baseClazz: ClassInfo, againstClazz: ClassInfo) {
    val meths = baseClazz.concreteMethods
    for (meth <- meths) {

      val p = analyzeMethod(AnalyzeTraitImplMethod(classVersion) _)(meth, againstClazz)
      p
    }
  }

  private def checkDeferredMethods(classVersion: Problem.ClassVersion.Value, baseClazz: ClassInfo, againstClazz: ClassInfo) {
    val meths = baseClazz.deferredMethods
    for (meth <- meths)
      analyzeMethod(AnalyzeDeferredMethod(classVersion) _)(meth, againstClazz)
  }

  override protected def checkNewMethods() {
    for (newmeth <- newClazz.concreteMethods if !oldClazz.hasStaticImpl(newmeth)) {
      if (!oldClazz.lookupMethods(newmeth.name).exists(_.sig == newmeth.sig)) {
        // this means that the method is brand new and therefore the implementation 
        // has to be injected 
        val problem = MissingMethodProblem(newmeth)(Problem.ClassVersion.Old)
        problem.status = Problem.Status.Upgradable
        raise(problem)
      }
      // else a static implementation for the same method existed already, therefore 
      // class that mixed-in the trait already have a forwarder to the implementation 
      // class. Mind that despite no binary incompatibility arises, but program's 
      // semantic may be severely affected.
    }

    for (newmeth <- newClazz.deferredMethods) {
      val oldmeths = oldClazz.lookupMethods(newmeth.name)
      oldmeths find (_.sig == newmeth.sig) match {
        case Some(oldmeth) =>
          ;
        case _ =>
          val problem = MissingMethodProblem(newmeth)(Problem.ClassVersion.Old)
          problem.status = Problem.Status.Upgradable
          raise(problem)
      }
    }
  }
}