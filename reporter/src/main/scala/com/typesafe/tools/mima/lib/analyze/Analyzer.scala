package com.typesafe.tools.mima.lib.analyze

import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.lib.analyze.field.BaseFieldChecker
import com.typesafe.tools.mima.lib.analyze.method.BaseMethodChecker
import com.typesafe.tools.mima.lib.analyze.template.TemplateChecker

object Analyzer {
  def apply(oldclz: ClassInfo, newclz: ClassInfo): List[Problem] = {
    if (oldclz.isClass && newclz.isClass) new ClassAnalyzer().apply(oldclz, newclz)
    else new TraitAnalyzer().apply(oldclz, newclz)
  }
}

private[analyze] trait Analyzer extends Function2[ClassInfo, ClassInfo, List[Problem]] {

  import scala.language.implicitConversions

  implicit def option2list(v: Option[Problem]): List[Problem] = v match {
    case None    => Nil
    case Some(p) => List(p)
  }

  implicit def listOfOption2list(xs: List[Option[Problem]]): List[Problem] =
    xs collect { case Some(p) => p }

  protected val fieldChecker: BaseFieldChecker
  protected val methodChecker: BaseMethodChecker

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
    TemplateChecker(oldclazz, newclazz)

  def analyzeMembers(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    analyzeFields(oldclazz, newclazz) ::: analyzeMethods(oldclazz, newclazz)

  def analyzeFields(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    for (oldfld <- oldclazz.fields.iterator.toList) yield fieldChecker.check(oldfld, newclazz)
  }

  def analyzeMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] =
    analyzeOldClassMethods(oldclazz, newclazz) ::: analyzeNewClassMethods(oldclazz, newclazz)

  /** Analyze incompatibilities that may derive from methods in the `oldclazz`*/
  def analyzeOldClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    for (oldmeth <- oldclazz.methods.iterator.toList) yield methodChecker.check(oldmeth, newclazz)
  }

  def analyzeNewClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem]
}

private[analyze] class ClassAnalyzer extends Analyzer {
  import com.typesafe.tools.mima.lib.analyze.field.ClassFieldChecker
  import com.typesafe.tools.mima.lib.analyze.method.ClassMethodChecker

  protected val fieldChecker = new ClassFieldChecker
  protected val methodChecker = new ClassMethodChecker

  override def analyze(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    if (oldclazz.isImplClass)
      Nil // do not analyze trait's implementation classes
    else
      super.analyze(oldclazz, newclazz)
  }

  /** Analyze incompatibilities that may derive from methods in the `newclazz` */
  override def analyzeNewClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    for (newAbstrMeth <- newclazz.deferredMethods) yield {
      oldclazz.lookupMethods(newAbstrMeth.bytecodeName).find(_.sig == newAbstrMeth.sig) match {
        case None =>
          Some(ReversedMissingMethodProblem(newAbstrMeth))
        case Some(found) =>
          if(found.isConcrete) {
        	Some(ReversedAbstractMethodProblem(newAbstrMeth))
          }
          else
        	None
      }
    }
  }
}

private[analyze] class TraitAnalyzer extends Analyzer {
  import com.typesafe.tools.mima.lib.analyze.field.ClassFieldChecker
  import com.typesafe.tools.mima.lib.analyze.method.TraitMethodChecker

  protected val fieldChecker = new ClassFieldChecker
  protected val methodChecker = new TraitMethodChecker

  override def analyzeNewClassMethods(oldclazz: ClassInfo, newclazz: ClassInfo): List[Problem] = {
    val res = collection.mutable.ListBuffer.empty[Problem]

    for (newmeth <- newclazz.concreteMethods if !oldclazz.hasStaticImpl(newmeth)) {
      if (!oldclazz.lookupMethods(newmeth.bytecodeName).exists(_.sig == newmeth.sig)) {
        // this means that the method is brand new and therefore the implementation
        // has to be injected
        val problem = ReversedMissingMethodProblem(newmeth)
        res += problem
      }
      // else a static implementation for the same method existed already, therefore
      // class that mixed-in the trait already have a forwarder to the implementation
      // class. Mind that, despite no binary incompatibility arises, program's
      // semantic may be severely affected.
    }

    for (newmeth <- newclazz.deferredMethods) {
      val oldmeths = oldclazz.lookupMethods(newmeth.bytecodeName)
      oldmeths find (_.sig == newmeth.sig) match {
        case Some(oldmeth) => ()
        case _ =>
          val problem = ReversedMissingMethodProblem(newmeth)
          res += problem
      }
    }

    res.toList
  }
}
