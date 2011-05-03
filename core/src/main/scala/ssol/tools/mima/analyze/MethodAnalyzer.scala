package ssol.tools.mima.analyze

import ssol.tools.mima._

trait MethodsAnalyzerHelper {
  protected def uniques(methods: List[MemberInfo]): List[MemberInfo] =
    methods.groupBy(_.parametersSig).values.map(_.head).toList
}

class MethodsAnalyzer extends Analyzer[List[MemberInfo], MemberInfo => List[MemberInfo]] with MethodsAnalyzerHelper {

  override protected def runAnalysis(leftMethods: List[MemberInfo], rightMethods: MemberInfo => List[MemberInfo]) {
    //FIXME[mirco]: We need to watch for flags. A public method that becomes private is an issue,
    //  while a public method that becomes a @bridge public method is ok! This need to be implemented!
    for (lmeth <- leftMethods) {
      analyzeMethod(lmeth, rightMethods)
    }
  }

  private def analyzeMethod(lmeth: MemberInfo, rightMethods: MemberInfo => List[MemberInfo]) {
    if (lmeth.isAccessible) {
      val rmeths = rightMethods(lmeth)
      if (rmeths.nonEmpty) {
        rmeths find (_.sig == lmeth.sig) match {
          case None =>
            rmeths find (lmeth matchesType _) match {
              case None =>
                raise(IncompatibleMethTypeProblem(lmeth, uniques(rmeths)))
              case Some(rmeth) =>
                raise(IncompatibleResultTypeProblem(lmeth, rmeth))
            }

          case Some(rmeth) =>
            if (!rmeth.isPublic)
              raise(InaccessibleMethodProblem(rmeth))
        }
      } 
      else raise(MissingMethodProblem(lmeth))
    }
  }
}