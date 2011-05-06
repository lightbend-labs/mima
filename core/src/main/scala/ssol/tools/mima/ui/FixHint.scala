package ssol.tools.mima.ui

import ssol.tools.mima._

sealed abstract class FixHint {
  def toSourceCode: String
}

case class AddBridgeMethod(oldmeth: MemberInfo, newmeth: MemberInfo) extends FixHint {
  override def toSourceCode = {
    
    val newMethResType = newmeth.tpe.asInstanceOf[MethodType].resultType
    val oldMethResType = oldmeth.tpe.asInstanceOf[MethodType].resultType
    /*val castNeeded = newMethResType.isSubtypeOf(oldMethResType)*/
    "@bridge " + oldmeth.defString + "(" + newmeth.applyString + ": " + newMethResType + ").asInstanceOf[" + oldMethResType + "]"
  }
}
