package com.typesafe.tools.mima.lib.analyze.template

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima._
import com.typesafe.tools.mima.lib.analyze.Rule

/*private[template]*/ object TemplateRules {
  trait TemplateRule extends Rule[ClassInfo, ClassInfo]

  object EntityDecl extends TemplateRule {
    def apply(thisClass: ClassInfo, thatClass: ClassInfo) = {
      if (thisClass.isClass == thatClass.isClass) None
      else if (thisClass.isInterface == thatClass.isInterface) None // traits are handled as interfaces
      else Some(IncompatibleTemplateDefProblem(thisClass, thatClass))
    }
  }

  object AbstractModifier extends TemplateRule {
    def apply(thisClass: ClassInfo, thatClass: ClassInfo) = {
      // a concrete class that is made abstract may entails binary incompatibility
      // because it can't be instantiated anymore
      if (thisClass.isConcrete && thatClass.isDeferred) Some(AbstractClassProblem(thisClass))
      // note: Conversely, an abstract class that is made concrete entails no issue
      else None
    }
  }

  object FinalModifier extends TemplateRule {
    def apply(thisClass: ClassInfo, thatClass: ClassInfo) = {
      // A non-final class that is made final entails a binary incompatibilities because client
      // code may be subclassing it
      if (thisClass.nonFinal && thatClass.isFinal) Some(FinalClassProblem(thisClass))
      // note: Conversely, a final class that is made non-final entails no issue
      else None
    }
  }

  object AccessModifier extends TemplateRule {
    def apply(thisClass: ClassInfo, thatClass: ClassInfo) = {
      if (thatClass.isLessVisibleThan(thisClass)) Some(InaccessibleClassProblem(thatClass))
      else None
    }
  }

  private[TemplateRules] trait ClassTypesHelper {
    def diff(thisTypes: Iterable[ClassInfo], thatTypes: Iterable[ClassInfo]) = {
      val thisSuperclasses = thisTypes.map(_.fullName).toSet
      val thatSuperclasses = thisTypes.map(_.fullName).toSet

      thisTypes.filter(sc => !thatTypes.exists(_.fullName == sc.fullName))
    }
  }

  object Superclasses extends TemplateRule with ClassTypesHelper {
    def apply(thisClass: ClassInfo, thatClass: ClassInfo) = {
      val missing = diff(thisClass.superClasses, thatClass.superClasses)

      if(missing.isEmpty)
        None
      else
        Some(MissingTypesProblem(thatClass, missing))
    }
  }

  object Superinterfaces extends TemplateRule with ClassTypesHelper {
    def apply(thisClass: ClassInfo, thatClass: ClassInfo) = {
      val missing = diff(thisClass.allInterfaces, thatClass.allInterfaces)

      if(missing.isEmpty)
        None
      else
        Some(MissingTypesProblem(thatClass, missing))
    }
  }

  object CyclicTypeReference extends TemplateRule {
    def apply(clz: ClassInfo) = {
      if(clz.superClasses.contains(clz))
        Some(CyclicTypeReferenceProblem(clz))
      else
        None
    }

    def apply(oldclz: ClassInfo, newclz: ClassInfo) = this(newclz)
  }
}
