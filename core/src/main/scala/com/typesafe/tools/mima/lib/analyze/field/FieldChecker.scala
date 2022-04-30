package com.typesafe.tools.mima.lib.analyze.field

import com.typesafe.tools.mima.core._

private[analyze] object FieldChecker {
  def check(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    for (oldfld <- oldclazz.fields.value; problem <- check1(oldfld, newclazz)) yield problem

  private def check1(oldfld: FieldInfo, newclazz: ClassInfo): Option[Problem] =
    if (oldfld.nonAccessible) None
    else {
      val newflds = newclazz.lookupClassFields(oldfld)
      if (newflds.hasNext) {
        val newfld = newflds.next()
        if (!newfld.isPublic) Some(InaccessibleFieldProblem(newfld))
        else if (oldfld.descriptor != newfld.descriptor) Some(IncompatibleFieldTypeProblem(oldfld, newfld))
        else if (oldfld.isStatic && !newfld.isStatic) Some(StaticVirtualMemberProblem(oldfld))
        else if (!oldfld.isStatic && newfld.isStatic) Some(VirtualStaticMemberProblem(oldfld))
        else None
      } else Some(MissingFieldProblem(oldfld))
    }
}
