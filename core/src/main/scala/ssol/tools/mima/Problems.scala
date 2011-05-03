package ssol.tools.mima

object Problem {
  object Status extends Enumeration {
    val Unfixable = Value("unfixable")
    val Upgradable = Value("upgradable") // means MiMa Client can fix the bytecode
    val SourceFixable = Value("source fixable") // means that the break may be easily fixed in the source
    val Ignored = Value("ignored")
  }
}

sealed abstract class Problem(val status: Problem.Status.Value = Problem.Status.Unfixable) {
  val description: String
}

case class MissingFieldProblem(oldfld: MemberInfo) extends Problem {
  override val description = oldfld.fieldString + " does not have a correspondent in new version"
}

case class MissingMethodProblem(oldmeth: MemberInfo) extends Problem(if (oldmeth.owner.isTrait) Problem.Status.Upgradable else Problem.Status.Unfixable) {
  override val description = oldmeth.methodString + " does not have a correspondent in new version"
}

case class MissingClassProblem(oldclazz: ClassInfo) extends Problem {
  override val description = oldclazz.classString + " does not have a correspondent in new version"
}

case class MissingPackageProblem(oldpkg: PackageInfo) extends Problem {
  override val description = oldpkg.packageString + " does not have a correspondent in new version"
}

case class InaccessibleFieldProblem(newfld: MemberInfo) extends Problem {
  override val description = newfld.fieldString + " was public; is inaccessible in new version"
}

case class InaccessibleMethodProblem(newmeth: MemberInfo) extends Problem {
  override val description = newmeth.methodString + " was public; is inaccessible in new version"
}

case class InaccessibleClassProblem(newclazz: ClassInfo) extends Problem {
  override val description = newclazz.classString + " was public; is inaccessible in new version"
}

case class IncompatibleFieldTypeProblem(oldfld: MemberInfo, newfld: MemberInfo) extends Problem {
  override val description = newfld.fieldString + "'s type has changed; was: " + oldfld.tpe + ", is now: " + newfld.tpe
}

case class IncompatibleMethTypeProblem(oldmeth: MemberInfo, newmeths: List[MemberInfo])(implicit status: Problem.Status.Value = Problem.Status.SourceFixable) extends Problem(status) {
  override val description = {
    oldmeth.methodString + (if (newmeths.tail.isEmpty)
      "'s type has changed; was " + oldmeth.tpe + ", is now: " + newmeths.head.tpe
    else
      " does not have a correspondent with same parameter signature among " +
        (newmeths map (_.tpe) mkString ", "))
  }
}

case class IncompatibleResultTypeProblem(oldmeth: MemberInfo, newmeth: MemberInfo)(implicit status: Problem.Status.Value = Problem.Status.SourceFixable) extends Problem(status) {
  override val description = {
    oldmeth.methodString + " has now a different result type; was: " +
      oldmeth.tpe.resultType + ", is now: " + newmeth.tpe.resultType
  }
}

case class AbstractMethodProblem(newmeth: MemberInfo) extends Problem(Problem.Status.Upgradable) {
  override val description = "abstract " + newmeth.methodString + " does not have a correspondent in old version"
}

case class IncompatibleClassDeclarationProblem(oldClazz: ClassInfo, newClazz: ClassInfo) extends Problem {
  override val description = {
    "declaration of " + oldClazz.description + " has changed to " + newClazz.description +
      " in new version; changing " + oldClazz.declarationPrefix + " to " + newClazz.declarationPrefix + " breaks client code"
  }
}