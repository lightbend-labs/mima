package com.typesafe.tools.mima.core

trait WithLocalModifier extends HasAccessFlags {
  def isConcrete: Boolean = !isDeferred

  def isDeferred: Boolean = ClassfileParser.isDeferred(flags)

  def isFinal: Boolean = ClassfileParser.isFinal(flags)

  def nonFinal: Boolean = !isFinal

  def isSynthetic: Boolean = ClassfileParser.isSynthetic(flags)

  def isBridge: Boolean = ClassfileParser.isBridge(flags)

  def nonBridge: Boolean = !isBridge
}
