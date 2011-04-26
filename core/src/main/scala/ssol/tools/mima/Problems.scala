package ssol.tools.mima

object Problem {
  object Status extends Enumeration {
    val Unfixable = Value("unfixable")
    val Upgradable = Value("upgradable") 
    val Fixable = Value("fixable")
    val ToFix = Value("to be fixed") 
    val Ignored = Value("ignored")
    val Fixed = Value("fixed")
  }
}

abstract class Problem(val description: String, initialStatus: Problem.Status.Value = Problem.Status.Unfixable) {
  var status: Problem.Status.Value = initialStatus
}

case class MissingFieldProblem(oldfld: MemberInfo) extends
  Problem(oldfld.fieldString+" does not have a correspondent in new version")

case class MissingMethodProblem(oldmeth: MemberInfo) extends
  Problem(oldmeth.methodString+" does not have a correspondent in new version")

case class MissingClassProblem(oldclazz: ClassInfo) extends
  Problem(oldclazz.classString+" does not have a correspondent in new version")

case class MissingPackageProblem(oldpkg: PackageInfo) extends
  Problem(oldpkg.packageString+" does not have a correspondent in new version")

case class InaccessibleFieldProblem(newfld: MemberInfo) extends
  Problem(newfld.fieldString+" was public; is inaccessible in new version")

case class InaccessibleMethodProblem(newmeth: MemberInfo) extends
  Problem(newmeth.methodString+" was public; is inaccessible in new version")

case class InaccessibleClassProblem(newclazz: ClassInfo) extends
  Problem(newclazz.classString+" was public; is inaccessible in new version")

case class IncompatibleFieldTypeProblem(oldfld: MemberInfo, newfld: MemberInfo) extends
  Problem(newfld.fieldString+"'s type has changed; was: "+oldfld.tpe+", is now: "+newfld.tpe)

case class IncompatibleMethTypeProblem(oldmeth: MemberInfo, newmeths: List[MemberInfo]) extends
  Problem(
    oldmeth.methodString+(
    if (newmeths.tail.isEmpty)
      "'s type has changed; was "+oldmeth.tpe+", is now: "+newmeths.head.tpe
    else
      "does not have a correspondent with same parameter signature among "+
      (newmeths map (_.tpe) mkString ", ")))

case class IncompatibleResultTypeProblem(oldmeth: MemberInfo, newmeth: MemberInfo) extends
  Problem(oldmeth.methodString+" has now a different result type; was: "+
          oldmeth.tpe.resultType+", is now: "+newmeth.tpe.resultType) {
  
  status = if(newmeth.tpe.isSubtypeOf(oldmeth.tpe)) Problem.Status.Fixable else Problem.Status.Unfixable  
}

case class AbstractMethodProblem(newmeth: MemberInfo) extends
  Problem("abstract "+newmeth.methodString+" does not have a correspondent in old version",
          if (newmeth.owner.isTrait && newmeth.owner.implClass.hasStaticImpl(newmeth)) Problem.Status.Upgradable
          else Problem.Status.Unfixable)
