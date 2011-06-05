package ssol.tools.mima.core

trait WithAccessModifier extends HasAccessFlags {

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
  
  def accessModifier = {
    if(isProtected) "protected"
    else if(isPrivate) "private"
    else ""
  }
}