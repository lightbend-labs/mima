package com.typesafe.tools.mima.lib.analyze

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.lib.analyze.field.BaseFieldChecker
import com.typesafe.tools.mima.lib.analyze.field.ClassFieldChecker
import com.typesafe.tools.mima.lib.analyze.method.BaseMethodChecker
import com.typesafe.tools.mima.lib.analyze.method.ClassMethodChecker
import com.typesafe.tools.mima.lib.analyze.method.TraitMethodChecker
import com.typesafe.tools.mima.lib.analyze.template.TemplateChecker

object Analyzer {
  def apply(oldclz: ClassInfo, newclz: ClassInfo): List[Problem] = {
    if (oldclz.isClass && newclz.isClass) new ClassAnalyzer().apply(oldclz, newclz)
    else new TraitAnalyzer().apply(oldclz, newclz)
  }
}

private[analyze] trait Analyzer extends ((ClassInfo, ClassInfo) => List[Problem]) {
  protected def fieldChecker: BaseFieldChecker
  protected def methodChecker: BaseMethodChecker

  def apply(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    analyze(oldclazz, newclazz)

  def analyze(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    assert(oldclazz.bytecodeName == newclazz.bytecodeName)
    val templateProblems = analyzeTemplateDecl(oldclazz, newclazz)

    if (templateProblems.exists(p => p.isInstanceOf[IncompatibleTemplateDefProblem] ||
      p.isInstanceOf[CyclicTypeReferenceProblem]))
      templateProblems // IncompatibleTemplateDefProblem implies major incompatibility, does not make sense to continue
    else
      templateProblems ::: analyzeMembers(oldclazz, newclazz)
  }

  def analyzeTemplateDecl(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    TemplateChecker(oldclazz, newclazz).toList

  def analyzeMembers(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    analyzeFields(oldclazz, newclazz) ::: analyzeMethods(oldclazz, newclazz)

  def analyzeFields(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    for {
      oldfld <- oldclazz.fields.iterator.toList
      p <- fieldChecker.check(oldfld, newclazz)
    } yield p
  }

  def analyzeMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    analyzeOldClassMethods(oldclazz, newclazz) ::: analyzeNewClassMethods(oldclazz, newclazz)

  /** Analyze incompatibilities that may derive from methods in the `oldclazz`*/
  def analyzeOldClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    for {
      oldmeth <- oldclazz.methods.iterator.toList
      p <- methodChecker.check(oldmeth, newclazz)
    } yield p
  }

  def analyzeNewClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem]

  protected def collectNewAbstractMethodsInNewInheritedTypes(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    def allInheritedTypes(clazz: ClassInfo) = clazz.superClasses ++ clazz.allInterfaces
    val oldInheritedTypes = allInheritedTypes(oldclazz)
    val newInheritedTypes = allInheritedTypes(newclazz)
    val diff = newInheritedTypes.diff(oldInheritedTypes)

    def noInheritedMatchingMethod(clazz: ClassInfo, deferredMethod: MethodInfo)(
        extraMethodMatchingCond: MemberInfo => Boolean
    ): Boolean = {
      val methods = clazz.lookupMethods(deferredMethod.bytecodeName)
      val matchingMethods = methods.filter(_.matchesType(deferredMethod))

      !matchingMethods.exists { method =>
        method.owner != deferredMethod.owner &&
        extraMethodMatchingCond(method)
      }
    }

    (for {
      tpe <- diff.iterator
      // if `tpe` is a trait, then the trait's concrete methods should be counted as deferred methods
      newDeferredMethod <- tpe.deferredMethodsInBytecode
         // checks that the newDeferredMethod did not already exist in one of the oldclazz supertypes 
      if noInheritedMatchingMethod(oldclazz, newDeferredMethod)(_ => true) &&
         // checks that no concrete implementation of the newDeferredMethod is provided by one of the newclazz supertypes
         noInheritedMatchingMethod(newclazz, newDeferredMethod)(_.isConcrete)
    } yield
       // report a binary incompatibility as there is a new inherited abstract method, which can lead to a AbstractErrorMethod at runtime
       InheritedNewAbstractMethodProblem(newclazz, newDeferredMethod)
    ).toList
  }
}

private[analyze] class ClassAnalyzer extends Analyzer {
  protected val fieldChecker = new ClassFieldChecker
  protected val methodChecker = new ClassMethodChecker

  override def analyze(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    if (oldclazz.isImplClass)
      Nil // do not analyze trait's implementation classes
    else
      super.analyze(oldclazz, newclazz)
  }

  /** Analyze incompatibilities that may derive from methods in the `newclazz`. */
  override def analyzeNewClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    (for {
      newAbstrMeth <- newclazz.deferredMethods
      problem <- oldclazz.lookupMethods(newAbstrMeth.bytecodeName).find(_.descriptor == newAbstrMeth.descriptor) match {
        case None        => Some(ReversedMissingMethodProblem(newAbstrMeth))
        case Some(found) => if (found.isConcrete) Some(ReversedAbstractMethodProblem(newAbstrMeth)) else None
      }
    } yield problem) ::: collectNewAbstractMethodsInNewInheritedTypes(oldclazz, newclazz)
  }
}

private[analyze] class TraitAnalyzer extends Analyzer {
  protected val fieldChecker = new ClassFieldChecker
  protected val methodChecker = new TraitMethodChecker

  override def analyzeNewClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    val res = scala.collection.mutable.ListBuffer.empty[Problem]

    for (newmeth <- newclazz.emulatedConcreteMethods if !oldclazz.hasStaticImpl(newmeth)) {
      if (!oldclazz.lookupMethods(newmeth.bytecodeName).exists(_.descriptor == newmeth.descriptor)) {
        // this means that the method is brand new and therefore the implementation
        // has to be injected
        res += ReversedMissingMethodProblem(newmeth)
      }
      // else a static implementation for the same method existed already, therefore
      // class that mixed-in the trait already have a forwarder to the implementation
      // class. Mind that, despite no binary incompatibility arises, program's
      // semantic may be severely affected.
    }

    for (newmeth <- newclazz.deferredMethods) {
      val oldmeths = oldclazz.lookupMethods(newmeth.bytecodeName)
      oldmeths.find(_.descriptor == newmeth.descriptor) match {
        case Some(_) => ()
        case None    => res += ReversedMissingMethodProblem(newmeth)
      }
    }

    res ++= collectNewAbstractMethodsInNewInheritedTypes(oldclazz, newclazz)
    res.toList
  }
}
