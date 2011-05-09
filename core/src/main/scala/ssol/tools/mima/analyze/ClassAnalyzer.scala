package ssol.tools.mima.analyze

import ssol.tools.mima._

private[analyze] class ClassAnalyzer extends BaseClassAnalyzer {
  override protected def runAnalysis(oldclazz: ClassInfo, newclazz: ClassInfo) {
    assert(oldclazz.isClass && newclazz.isClass) //not valid because interfaces are also analyzed here...
    if (oldclazz.isImplClass)
      Nil
    else
      super.runAnalysis(oldclazz, newclazz)
  }
}