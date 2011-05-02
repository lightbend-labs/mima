package ssol.tools.mima.analyze

import ssol.tools.mima._

trait MethodsAnalyzerHelper {
  protected def uniques(methods: List[MemberInfo]): List[MemberInfo] =
    methods.groupBy(_.parametersSig).values.map(_.head).toList
}

class MethodsAnalyzer extends Analyzer[List[MemberInfo], MemberInfo => List[MemberInfo]] with MethodsAnalyzerHelper {

  override def analyze(reporter: Reporter)(leftMethods: List[MemberInfo], rightMethods: MemberInfo => List[MemberInfo]) {
    //FIXME[mirco]: We need to watch for flags. A public method that becomes private is an issue,
    //  while a public method that becomes a @bridge public method is ok! This need to be implemented!
    for (lmeth <- leftMethods) {
      analyzeMethod(reporter)(lmeth, rightMethods)
    }
  }

  def analyzeMethod(reporter: Reporter)(lmeth: MemberInfo, rightMethods: MemberInfo => List[MemberInfo]) {
    if (lmeth.isAccessible) {
      val rmeths = rightMethods(lmeth)
      if (rmeths.nonEmpty) {
        rmeths find (_.sig == lmeth.sig) match {
          case None =>
            rmeths find (lmeth matchesType _) match {
              case None =>
                reporter.raise(IncompatibleMethTypeProblem(lmeth, uniques(rmeths)))
              case Some(rmeth) =>
                reporter.raise(IncompatibleResultTypeProblem(lmeth, rmeth))
            }

          case Some(rmeth) =>
            if (!rmeth.isPublic)
              reporter.raise(InaccessibleMethodProblem(rmeth))
        }
      } 
      else reporter.raise(MissingMethodProblem(lmeth))
    }
  }
}