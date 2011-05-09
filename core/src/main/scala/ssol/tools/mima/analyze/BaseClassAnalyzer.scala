package ssol.tools.mima.analyze

import ssol.tools.mima._
import Config._

private[analyze] abstract class BaseClassAnalyzer extends Analyzer {
  override protected def runAnalysis(oldclazz: ClassInfo, newclazz: ClassInfo) {
    assert(oldclazz.name == newclazz.name)
    InaccessibleClassCheck(oldclazz, newclazz) match {
      case None => analyzeMembers(oldclazz, newclazz)
      case Some(p) => raise(p)
    }  
  }

  private def analyzeMembers(oldclazz: ClassInfo, newclazz: ClassInfo) {
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
          /*if (!newfld.isPublic)
            raise(InaccessibleFieldProblem(newfld))
          else*/ if (oldfld.sig != newfld.sig)
            raise(IncompatibleFieldTypeProblem(oldfld, newfld))
        } else
          raise(MissingFieldProblem(oldfld))
      }
  }

  protected def analyzeMethods(oldclazz: ClassInfo, newclazz: ClassInfo) {
    analyzeOldClassMethods(oldclazz, newclazz)
    analyzeNewClassMethods(oldclazz, newclazz)
  }

  protected def analyzeOldClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo) {
    for (meth <- oldclazz.methods.iterator) 
      raise(MethodCheck(meth, newclazz))
  }

  protected def analyzeNewClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo) {
    for (newAbstrMeth <- newclazz.deferredMethods) {
      MethodCheck(newAbstrMeth, oldclazz) match {
        case Some(p) => 
          val p = MissingMethodProblem(newAbstrMeth)
          p.affectedVersion = Problem.ClassVersion.Old
          p.status = Problem.Status.Upgradable
          raise(p)
        case None => ()
      }
    }
  }
}