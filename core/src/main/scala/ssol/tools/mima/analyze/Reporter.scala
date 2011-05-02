package ssol.tools.mima.analyze

import ssol.tools.mima.Problem

private[analyze] trait Reporter {
  def problems: List[Problem]
  private[analyze] def raise(problem: Problem)
}