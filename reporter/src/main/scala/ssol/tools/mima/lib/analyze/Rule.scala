package ssol.tools.mima.lib.analyze

import ssol.tools.mima.core.Problem

private[analyze] trait Rule[T,S] extends Function2[T,S,Option[Problem]]