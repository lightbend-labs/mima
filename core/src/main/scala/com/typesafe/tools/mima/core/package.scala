package com.typesafe.tools.mima

package object core {

  /** Returns `true` for problems to keep, `false` for problems to drop. */
  type ProblemFilter = Problem => Boolean
}
