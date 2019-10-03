package com.typesafe.tools.mima.lib.analyze

import com.typesafe.tools.mima.core.Problem

private[analyze] trait Rule[T, S] {
  def run(t: T, s: S): Option[Problem]
}
