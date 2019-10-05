package com.typesafe.tools.mima.lib.analyze

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.lib.analyze.field.FieldChecker
import com.typesafe.tools.mima.lib.analyze.method.MethodChecker
import com.typesafe.tools.mima.lib.analyze.template.TemplateChecker

object Analyzer {
  def analyze(oldclz: ClassInfo, newclz: ClassInfo): List[Problem] = {
    if (oldclz.isClass && newclz.isClass) new ClassAnalyzer().analyze(oldclz, newclz)
    else new TraitAnalyzer().analyze(oldclz, newclz)
  }
}

private[analyze] trait Analyzer {
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
    TemplateChecker.check(oldclazz, newclazz).toList

  def analyzeMembers(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    FieldChecker.check(oldclazz, newclazz) ::: analyzeMethods(oldclazz, newclazz)

  def analyzeMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    MethodChecker.check(oldclazz, newclazz) ::: analyzeNewClassMethods(oldclazz, newclazz)

  def analyzeNewClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem]

  protected def collectNewAbstractMethodsInNewInheritedTypes(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    def allInheritedTypes(clazz: ClassInfo) = clazz.superClasses ++ clazz.allInterfaces
    val oldInheritedTypes = allInheritedTypes(oldclazz)
    val newInheritedTypes = allInheritedTypes(newclazz)
    val diff = newInheritedTypes.diff(oldInheritedTypes)

    def noInheritedMatchingMethod(clazz: ClassInfo, deferredMethod: MethodInfo)(
        extraMethodMatchingCond: MemberInfo => Boolean
    ): Boolean = {
      val methods = clazz.lookupMethods(deferredMethod)
      val matchingMethods = methods.filter(_.matchesType(deferredMethod))

      !matchingMethods.exists { method =>
        method.owner != deferredMethod.owner &&
        extraMethodMatchingCond(method)
      }
    }

    (for {
      newInheritedType <- diff.iterator
      // if `newInheritedType` is a trait, then the trait's concrete methods should be counted as deferred methods
      newDeferredMethod <- newInheritedType.deferredMethodsInBytecode
         // checks that the newDeferredMethod did not already exist in one of the oldclazz supertypes 
      if noInheritedMatchingMethod(oldclazz, newDeferredMethod)(_ => true) &&
         // checks that no concrete implementation of the newDeferredMethod is provided by one of the newclazz supertypes
         noInheritedMatchingMethod(newclazz, newDeferredMethod)(_.isConcrete)
    } yield {
       // report a binary incompatibility as there is a new inherited abstract method, which can lead to a AbstractErrorMethod at runtime
       val newmeth = new MethodInfo(newclazz, newDeferredMethod.bytecodeName, newDeferredMethod.flags, newDeferredMethod.descriptor)
       InheritedNewAbstractMethodProblem(newDeferredMethod, newmeth)
    }).toList
  }
}

private[analyze] class ClassAnalyzer extends Analyzer {
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
      problem <- oldclazz.lookupMethods(newAbstrMeth).find(_.descriptor == newAbstrMeth.descriptor) match {
        case None        => Some(ReversedMissingMethodProblem(newAbstrMeth))
        case Some(found) => if (found.isConcrete) Some(ReversedAbstractMethodProblem(newAbstrMeth)) else None
      }
    } yield problem) ::: collectNewAbstractMethodsInNewInheritedTypes(oldclazz, newclazz)
  }
}

private[analyze] class TraitAnalyzer extends Analyzer {
  override def analyzeNewClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    val res = scala.collection.mutable.ListBuffer.empty[Problem]

    for (newmeth <- newclazz.emulatedConcreteMethods if !oldclazz.hasStaticImpl(newmeth)) {
      if (!oldclazz.lookupMethods(newmeth).exists(_.descriptor == newmeth.descriptor)) {
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
      val oldmeths = oldclazz.lookupMethods(newmeth)
      oldmeths.find(_.descriptor == newmeth.descriptor) match {
        case Some(_) => ()
        case None    => res += ReversedMissingMethodProblem(newmeth)
      }
    }

    res ++= collectNewAbstractMethodsInNewInheritedTypes(oldclazz, newclazz)
    res.toList
  }
}
