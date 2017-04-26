package com.typesafe.tools.mima.lib.analyze.field

import com.typesafe.tools.mima.lib.analyze.Checker
import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima._

private[analyze] abstract class BaseFieldChecker extends Checker[MemberInfo, ClassInfo] {

  def check(field: MemberInfo, in: ClassInfo): Option[Problem] = {
    if (field.isAccessible) {
      val newflds = in.lookupClassFields(field.bytecodeName)
      if (newflds.hasNext) {
        val newfld = newflds.next
        if (!newfld.isPublic)
          Some(InaccessibleFieldProblem(newfld))
        else if(field.sig != newfld.sig)
          Some(IncompatibleFieldTypeProblem(field, newfld))
        else if (field.isStatic && !newfld.isStatic)
          Some(StaticVirtualMemberProblem(field))
        else if (!field.isStatic && newfld.isStatic)
          Some(VirtualStaticMemberProblem(field))
        else
          None
      }
      else Some(MissingFieldProblem(field))
    }
    else None
  }
}

private[analyze] class ClassFieldChecker extends BaseFieldChecker