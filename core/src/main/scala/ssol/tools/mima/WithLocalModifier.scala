package ssol.tools.mima

trait WithLocalModifier extends HasAccessFlags {
  def isDeferred: Boolean = ClassfileParser.isDeferred(flags)
  
  def isFinal: Boolean = ClassfileParser.isFinal(flags)
}