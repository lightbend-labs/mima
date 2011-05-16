package ssol.tools.mima

trait WithLocalModifier extends HasAccessFlags {
  def isConcrete: Boolean = !isDeferred
  
  def isDeferred: Boolean = ClassfileParser.isDeferred(flags)
  
  def isFinal: Boolean = ClassfileParser.isFinal(flags)
  
  def nonFinal: Boolean = !isFinal
}