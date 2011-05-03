package ssol.tools.mima.analyze

import ssol.tools.mima._

private class ClassAnalyzer extends BaseClassAnalyzer {
  override protected def runAnalysis(oldClazz: ClassInfo, newClazz: ClassInfo) {
    //assert(oldClazz.isClass && newClazz.isClass) not valid because interfaces are also analyzed here...
    // never go into implementation classes, as errors should be reported by the `TraitAnalyzer`
    if (oldClazz.isImplClass)
      None 
    else
      super.runAnalysis(oldClazz, newClazz)
  }
}