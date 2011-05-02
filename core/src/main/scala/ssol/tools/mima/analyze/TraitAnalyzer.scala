package ssol.tools.mima.analyze

import ssol.tools.mima._

private class TraitAnalyzer extends BaseClassAnalyzer {

  /*
  override def analyze(oldClazz: ClassInfo, newClazz: ClassInfo): Option[List[Problem]] = {
    assert(oldClazz.isTrait && newClazz.isTrait, "either `" + oldClazz.fullName + "` or `"+
         newClazz.fullName + "` are not traits")
    super.analyze(oldClazz, newClazz)
  }*/

  override protected def checkOldMethods(reporter: Reporter)(oldclazz: ClassInfo, newclazz: ClassInfo) {
    checkConcreteMethods(reporter)(oldclazz, newclazz)
    checkDeferredMethods(reporter)(oldclazz, newclazz)
  }

  private def checkConcreteMethods(reporter: Reporter)(oldclazz: ClassInfo, newclazz: ClassInfo) {
    val oldmeths = if(oldclazz.isTrait) oldclazz.concreteMethods else Nil
    val newmeths = (old: MemberInfo) => if(newclazz.isTrait) newclazz.concreteMethods.filter(_.name == old.name) else Nil
    
    
    //checkMethods(reporter)(oldmeths, newmeths)
    
    val methodAnalyzer = new MethodsAnalyzer
  	methodAnalyzer.analyze(oldmeths, newmeths) match {
  	  case None => ()
  	  case Some(problems) => 
  	    for(problem <- problems) problem match {
  	      case IncompatibleResultTypeProblem(oldmeth, newmeth) =>
  	        methodAnalyzer.analyze(List(oldmeth), (meth: MemberInfo) => newclazz.lookupMethods(meth.name).toList) match {
  	          case None => reporter.raise(IncompatibleResultTypeProblem(oldmeth, newmeth)(Problem.Status.Upgradable))
  	          case Some(p) => reporter.raise(problem)
  	        }
  	      
  	      case IncompatibleMethTypeProblem(oldmeth, newmeths) => 
  	        methodAnalyzer.analyze(List(oldmeth), (meth: MemberInfo) => newclazz.lookupMethods(meth.name).toList) match {
  	          case None => reporter.raise(IncompatibleMethTypeProblem(oldmeth, newmeths)(Problem.Status.Upgradable))
  	          case Some(p) => reporter.raise(problem)
  	        }
  	        
  	      case _ => reporter.raise(problem)
  	    }
  	}   
  }
  
  private def checkDeferredMethods(reporter: Reporter)(oldclazz: ClassInfo, newclazz: ClassInfo) {
    checkMethods(reporter)(if(oldclazz.isTrait) oldclazz.methods.iterator.toList -- oldclazz.concreteMethods else oldclazz.methods.iterator.toList, 
        oldMeth => newclazz.lookupMethods(oldMeth.name).toList)
  }
}