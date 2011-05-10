package ssol.tools.mima.analyze

import ssol.tools.mima._

private[analyze] class TraitAnalyzer extends BaseClassAnalyzer {

  override protected def analyzeNewClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo) {
    for (newmeth <- newclazz.concreteMethods if !oldclazz.hasStaticImpl(newmeth)) {
      if (!oldclazz.lookupMethods(newmeth.name).exists(_.sig == newmeth.sig)) {
        // this means that the method is brand new and therefore the implementation 
        // has to be injected 
        val problem = MissingMethodProblem(newmeth)
        problem.affectedVersion = Problem.ClassVersion.Old
        problem.status = Problem.Status.Upgradable
        raise(problem)
      }
      // else a static implementation for the same method existed already, therefore 
      // class that mixed-in the trait already have a forwarder to the implementation 
      // class. Mind that, despite no binary incompatibility arises, program's 
      // semantic may be severely affected.
    }

    
    for (newmeth <- newclazz.deferredMethods) {
      val oldmeths = oldclazz.lookupMethods(newmeth.name)
      oldmeths find (_.sig == newmeth.sig) match {
        case Some(oldmeth) =>
          ;
        case _ =>
          val problem = MissingMethodProblem(newmeth)
          problem.status = Problem.Status.Upgradable
          problem.affectedVersion = Problem.ClassVersion.Old
          raise(problem)
      }
    }
  }
}