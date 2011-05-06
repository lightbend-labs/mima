package ssol.tools.mima.analyze

import ssol.tools.mima._

private class ClassAnalyzer(oldClazz: ClassInfo, newClazz: ClassInfo) extends BaseClassAnalyzer(oldClazz, newClazz) {
  override protected def runAnalysis() {
    //assert(oldClazz.isClass && newClazz.isClass) not valid because interfaces are also analyzed here...
    // never go into implementation classes, as errors should be reported by the `TraitAnalyzer`
    if (oldClazz.isImplClass)
      Nil
    else
      super.runAnalysis()
  }
}