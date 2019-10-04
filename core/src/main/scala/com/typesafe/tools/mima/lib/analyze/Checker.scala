package com.typesafe.tools.mima.lib.analyze

import com.typesafe.tools.mima.core.Problem

private[analyze] trait Checker[T, S] {
  def check(thisEl: T, thatEl: S): Option[Problem]
}
