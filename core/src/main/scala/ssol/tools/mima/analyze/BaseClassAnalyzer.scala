package ssol.tools.mima.analyze

import ssol.tools.mima._
import Config._
import MethodAnalyzer._

private abstract class BaseClassAnalyzer(oldClazz: ClassInfo, newClazz: ClassInfo) extends Analyzer {

  override protected def runAnalysis() {
    assert(oldClazz.name == newClazz.name)
    if (oldClazz.isPublic && !newClazz.isPublic)
      raise(InaccessibleClassProblem(newClazz))
    else {
      analyzeMembers()
    }
  }

  private def analyzeMembers() {
    info("[compare] %s \t %s".format(oldClazz, newClazz))

    analyzeFields()
    analyzeMethods()
  }

  protected def analyzeFields() {
    // comparing fields in old class versus new class
    for (oldfld <- oldClazz.fields.iterator)
      if (oldfld.isAccessible) {
        val newflds = newClazz.lookupClassFields(oldfld.name)
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

  protected def analyzeMethods() {
    checkOldMethods()
    checkNewMethods()
  }

  protected def analyzeMethod(analyzer: Function2[MemberInfo, ClassInfo, Option[Problem]])(m: MemberInfo, clazz: ClassInfo) {
    analyzer(m, clazz) match {
      case Some(p) => raise(p)
      case _       => ()
    }
  }

  protected def checkOldMethods() {
    // Forall(oldClazz.methods).Exists
    
    for (oldmeth <- oldClazz.methods.iterator.toList)
      analyzeMethod(AnalyzeClassMethod(Problem.ClassVersion.New) _)(oldmeth, newClazz)
  }

  protected def checkNewMethods() {
    for (newmeth <- newClazz.methods.iterator if newmeth.isDeferred) {
      val oldmeths = oldClazz.lookupMethods(newmeth.name)
      oldmeths find (_.sig == newmeth.sig) match {
        case Some(oldmeth) =>
          ;
        case _ =>
          raise(MissingMethodProblem(newmeth)(Problem.ClassVersion.Old))
      }
    }
  }

}