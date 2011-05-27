package ssol.tools.mima.lib.analyze

import ssol.tools.mima.core.Problem

private[analyze] trait Checker[T, S] extends Function2[T,S,Option[Problem]]{
  
  final override def apply(thisEl: T, thatEl: S): Option[Problem] = check(thisEl, thatEl)
  
  def check(thisEl: T, thatEl: S): Option[Problem]
  
  protected def checkRules[T,S](rules: Seq[Rule[T, S]])(oldEl: T, newEl: S): Option[Problem] = {
    if (rules.isEmpty) None
    else {
      val rule = rules.first
      rule(oldEl, newEl) match {
        case None    => checkRules(rules.tail)(oldEl, newEl)
        case res @ _ => res
      }
    }
  }
}