package ssol.tools.mima.analyze.field

import ssol.tools.mima.analyze.Checker
import ssol.tools.mima._

private[analyze] abstract class BaseFieldChecker extends Checker[MemberInfo, ClassInfo] {

  def check(field: MemberInfo, in: ClassInfo): Option[Problem] = {
    if (field.isAccessible) {
      val newflds = in.lookupClassFields(field.name)
      if (newflds.hasNext) {
        val newfld = newflds.next
        if (!newfld.isPublic)
          Some(InaccessibleFieldProblem(newfld))
        else if(field.sig != newfld.sig)
          Some(IncompatibleFieldTypeProblem(field, newfld))
        else 
          None
      } 
      else Some(MissingFieldProblem(field))
    }
    else None
  }
}

private[analyze] object ClassFieldChecker extends BaseFieldChecker