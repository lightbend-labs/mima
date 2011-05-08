package ssol.tools.mima

trait WithAccessModifier {

  protected def flags: Int

  def isPublic: Boolean = {
    ensureLoaded()
    ClassfileParser.isPublic(flags)
  }

  protected def isProtected: Boolean = {
    ensureLoaded()
    ClassfileParser.isProtected(flags)
  }

  protected def isPrivate: Boolean = {
    ensureLoaded()
    ClassfileParser.isPrivate(flags)
  }
  
  def isLessVisibleThan(that: WithAccessModifier) = {
    (!isPublic && that.isPublic) || (isPrivate && that.isProtected) 
  }
  
  protected def ensureLoaded() {}

}