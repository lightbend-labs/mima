package ssol.tools.mima.analyze

import ssol.tools.mima.Problem

private[analyze] trait Rule[T,S] extends Function2[T,S,Option[Problem]]