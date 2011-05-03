package ssol.tools.mima.analyze

import ssol.tools.mima._
import Config._

private class BaseClassAnalyzer extends ClassInfoAnalyzer {
  override protected def runAnalysis(oldClazz: ClassInfo, newClazz: ClassInfo) {
    assert(oldClazz.name == newClazz.name)
    if (oldClazz.isPublic && !newClazz.isPublic)
      raise(InaccessibleClassProblem(newClazz))
    else {
      analyseClasses(oldClazz, newClazz)
    }
  }

  private def analyseClasses(oldclazz: ClassInfo, newclazz: ClassInfo) {
    info("[compare] %s \t %s".format(oldclazz, newclazz))

    analyzeFields(oldclazz, newclazz)
    analyzeMethods(oldclazz, newclazz)
  }

  protected def analyzeFields(oldclazz: ClassInfo, newclazz: ClassInfo) {
    // comparing fields in old class versus new class
    for (oldfld <- oldclazz.fields.iterator)
      if (oldfld.isAccessible) {
        val newflds = newclazz.lookupClassFields(oldfld.name)
        if (newflds.hasNext) {
          val newfld = newflds.next
          if (!newfld.isPublic)
            raise(InaccessibleFieldProblem(newfld))
          else if (oldfld.sig != newfld.sig)
            raise(IncompatibleFieldTypeProblem(oldfld, newfld))
        } else
          raise(MissingFieldProblem(oldfld))
      }
  }

  protected def analyzeMethods(oldclazz: ClassInfo, newclazz: ClassInfo) {
    checkOldMethods(oldclazz, newclazz)
    checkNewMethods(oldclazz, newclazz)
  }

  protected def checkOldMethods(oldclazz: ClassInfo, newclazz: ClassInfo) {
    checkMethods(oldclazz.methods.iterator.toList, 
        oldMeth => newclazz.lookupMethods(oldMeth.name).toList)
  }

  protected def checkMethods(oldMeths: List[MemberInfo], newMeths: MemberInfo => List[MemberInfo]) {
  	val methodAnalyzer = new MethodsAnalyzer
  	methodAnalyzer.analyze(oldMeths, newMeths) match {
  	  case None => ()
  	  case Some(problems) => problems.foreach(raise)
  	}
  }

  protected def checkNewMethods(oldclazz: ClassInfo, newclazz: ClassInfo) {
    // reporting abstract methods defined in new class
    for (newmeth <- newclazz.methods.iterator)
      if (newmeth.isDeferred) {
        val oldmeths = oldclazz.lookupMethods(newmeth.name)
        oldmeths find (oldmeth => oldmeth.isDeferred && oldmeth.sig == newmeth.sig) match {
          case Some(oldmeth) => ()

          case _ =>
            raise(AbstractMethodProblem(newmeth))
        }
      }
  }

}