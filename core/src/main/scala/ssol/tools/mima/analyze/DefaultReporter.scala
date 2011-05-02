package ssol.tools.mima.analyze

import ssol.tools.mima.Problem

private[analyze] class DefaultReporter extends Reporter {
  private lazy val _problems = collection.mutable.ListBuffer.empty[Problem]
  def problems: List[Problem] = _problems.toList
  
  
  private[analyze] def raise(problem: Problem) = _problems += problem
}