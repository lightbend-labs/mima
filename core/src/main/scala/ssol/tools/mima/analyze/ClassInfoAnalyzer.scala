package ssol.tools.mima.analyze

import ssol.tools.mima.{ClassInfo, Problem, IncompatibleClassDeclarationProblem}

object ClassInfoAnalyzer { 
 
  def apply(oldClazz: ClassInfo, newClazz: ClassInfo): List[Problem] = {
    def analyze() = {
      //FIXME[mirco]: what about other declaration at the class level? Any special case to watch out...
      if( (oldClazz.isClass && newClazz.isTrait) || (oldClazz.isTrait && newClazz.isClass) ) {
        // don't go any further as this is a major breaking change...
        List(IncompatibleClassDeclarationProblem(oldClazz, newClazz))
      }
      
      // FIXME[mirco]: should we handle java interface and scala trait separately? Need to think about it... 
      else if(oldClazz.isTrait || newClazz.isTrait)  
        new TraitAnalyzer(oldClazz, newClazz).analyze()
      
      //FIXME[mirco]: What if a class is a trait and newclazz is a java interface?
      
      // FIXME[mirco]; Should we make a distinction between traits and interfaces?
      else new ClassAnalyzer(oldClazz, newClazz).analyze()
      }
    
    analyze().distinct
  }
}