package ssol.tools.mima.analyze

import ssol.tools.mima._
import Config._

private class BaseClassAnalyzer extends ClassInfoAnalyzer {
  override protected def analyze(reporter: Reporter)(oldClazz: ClassInfo, newClazz: ClassInfo) {
    assert(oldClazz.name == newClazz.name)
    if (oldClazz.isPublic && !newClazz.isPublic)
      reporter.raise(InaccessibleClassProblem(newClazz))
    else {
      runAnalysis(reporter)(oldClazz, newClazz)
    }
  }

  private def runAnalysis(reporter: Reporter)(oldclazz: ClassInfo, newclazz: ClassInfo) {
    info("[compare] %s \t %s".format(oldclazz, newclazz))

    analyzeFields(reporter)(oldclazz, newclazz)
    analyzeMethods(reporter)(oldclazz, newclazz)
  }

  protected def analyzeFields(reporter: Reporter)(oldclazz: ClassInfo, newclazz: ClassInfo) {
    // comparing fields in old class versus new class
    for (oldfld <- oldclazz.fields.iterator)
      if (oldfld.isAccessible) {
        val newflds = newclazz.lookupClassFields(oldfld.name)
        if (newflds.hasNext) {
          val newfld = newflds.next
          if (!newfld.isPublic)
            reporter.raise(InaccessibleFieldProblem(newfld))
          else if (oldfld.sig != newfld.sig)
            reporter.raise(IncompatibleFieldTypeProblem(oldfld, newfld))
        } else
          reporter.raise(MissingFieldProblem(oldfld))
      }
  }

  protected def analyzeMethods(reporter: Reporter)(oldclazz: ClassInfo, newclazz: ClassInfo) {
    checkOldMethods(reporter)(oldclazz, newclazz)
    checkNewMethods(reporter)(oldclazz, newclazz)
  }

  protected def checkOldMethods(reporter: Reporter)(oldclazz: ClassInfo, newclazz: ClassInfo) {
    checkMethods(reporter)(oldclazz.methods.iterator.toList, 
        oldMeth => newclazz.lookupMethods(oldMeth.name).toList)
  }

  protected def checkMethods(reporter: Reporter)(oldMeths: List[MemberInfo], newMeths: MemberInfo => List[MemberInfo]) {
  	val methodAnalyzer = new MethodsAnalyzer
  	methodAnalyzer.analyze(oldMeths, newMeths) match {
  	  case None => ()
  	  case Some(problems) => problems.foreach(reporter.raise)
  	}
  }

  protected def checkNewMethods(reporter: Reporter)(oldclazz: ClassInfo, newclazz: ClassInfo) {
    // reporting abstract methods defined in new class
    for (newmeth <- newclazz.methods.iterator)
      if (newmeth.isDeferred) {
        val oldmeths = oldclazz.lookupMethods(newmeth.name)
        oldmeths find (oldmeth => oldmeth.isDeferred && oldmeth.sig == newmeth.sig) match {
          case Some(oldmeth) => ()

          case _ =>
            reporter.raise(AbstractMethodProblem(newmeth))
        }
      }
  }

}