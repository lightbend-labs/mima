package com.typesafe.tools.mima.lib.analyze

import com.typesafe.tools.mima.core.Problem

private[analyze] trait Checker[T, S] {
  def check(thisEl: T, thatEl: S): Option[Problem]

  protected def checkRules[T1,S1](rules: Seq[Rule[T1, S1]])(oldEl: T1, newEl: S1): Option[Problem] = {
    rules match {
      case Seq()                 => None
      case Seq(rule, rules @ _*) => rule.run(oldEl, newEl).orElse(checkRules(rules)(oldEl, newEl))
    }
  }
}
