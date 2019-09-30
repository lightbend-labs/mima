package com.typesafe.tools.mima.core

import scala.reflect.NameTransformer

private[core] abstract class InfoLike {
  /** The name as found in the bytecode. */
  def bytecodeName: String

  protected def flags: Int

  /** The name as found in the original Scala source. */
  final def decodedName: String = NameTransformer.decode(bytecodeName)

  final def isPublic: Boolean    = ClassfileParser.isPublic(flags)
  final def isPrivate: Boolean   = ClassfileParser.isPrivate(flags)
  final def isProtected: Boolean = ClassfileParser.isProtected(flags)
  final def isStatic: Boolean    = ClassfileParser.isStatic(flags)
  final def isFinal: Boolean     = ClassfileParser.isFinal(flags)
  final def isDeferred: Boolean  = ClassfileParser.isDeferred(flags)
  final def isSynthetic: Boolean = ClassfileParser.isSynthetic(flags)

  final def nonPublic: Boolean    = !isPublic
  final def nonFinal: Boolean     = !isFinal
  final def isConcrete: Boolean   = !isDeferred

  final def isLessVisibleThan(that: InfoLike) = {
    (nonPublic && that.isPublic) || (isPrivate && that.isProtected)
  }
}
