package ssol.tools.mima.analyze

import ssol.tools.mima.{ClassInfo, Problem, IncompatibleClassDeclarationProblem}

/** common interface for analyzing classes. */
trait ClassInfoAnalyzer extends Analyzer[ClassInfo, ClassInfo] {
  
}

object ClassInfoAnalyzer {
  
  // need a new instance each time because `BaseClassAnalyzer` is mutable (and both analyzer inherith from it)
  private def classAnalyzer = new ClassAnalyzer
  private def traitAnalyzer = new TraitAnalyzer
  
  def apply(oldClazz: ClassInfo, newClazz: ClassInfo): Option[List[Problem]] = {
    //FIXME[mirco]: what about other declaration at the class level? Any special case to watch out...
    if( (oldClazz.isClass && newClazz.isTrait) || (oldClazz.isTrait && newClazz.isClass) ) {
      // don't go any further as this is a major breaking change...
      Some(IncompatibleClassDeclarationProblem(oldClazz, newClazz) :: Nil)
    }
    
    // FIXME[mirco]: should we handle java interface and scala trait separately? Need to think about it... 
    else if(oldClazz.isTrait || newClazz.isTrait) 
      traitAnalyzer.analyze(oldClazz, newClazz)
    
    //FIXME[mirco]: What if oldclazz is a trait and new clazz is interface?
    
    // FIXME[mirco]; Should we make a distinction between traits and interfaces?
    else classAnalyzer.analyze(oldClazz, newClazz)
  }
}