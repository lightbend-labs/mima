package ssol.tools.mima.ui

import ssol.tools.mima._

object FixHint {
  def apply(problem: Problem): Option[FixHint] = problem match {
    case IncompatibleResultTypeProblem(oldmeth, newmeth) =>
      if (problem.status == Problem.Status.Fixable)
        Some(AddBridgeMethod(oldmeth, newmeth))
      else None
    case _ => None
  }
}

abstract class FixHint {
  def toSourceCode: String
}

private case class AddBridgeMethod(oldmeth: MemberInfo, newmeth: MemberInfo) extends FixHint {
  override def toSourceCode = {
    val newMethResType = newmeth.tpe.asInstanceOf[MethodType].resultType
    val oldMethResType = oldmeth.tpe.asInstanceOf[MethodType].resultType
    "@bridge " + sourceMethod(oldmeth) + " = (" + newmeth.name + sourceParams(oldmeth) + ": " + newMethResType + ").asInstanceOf[" + oldMethResType + "]"
  }
  
  
  private def sourceMethod(meth: MemberInfo) = 
    "def "+ meth.name + sourceParams(meth) + ": " + meth.tpe.resultType
  
  private def sourceParams(meth: MemberInfo): String = meth.tpe match {
      case MethodType(paramTypes, resultType) => 
        val params = for((ptype, index) <- paramTypes.zipWithIndex) yield "par"+index+": "+ptype
        params.mkString("(", ",", ")")
  }
}
