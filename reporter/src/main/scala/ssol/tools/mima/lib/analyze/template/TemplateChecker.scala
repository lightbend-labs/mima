package ssol.tools.mima.lib.analyze.template

import ssol.tools.mima.core._
import ssol.tools.mima._
import ssol.tools.mima.lib.analyze.Checker

private[analyze] object TemplateChecker extends Checker[ClassInfo, ClassInfo] {

  import TemplateRules._
  
  protected val rules: Seq[TemplateRule] = Seq(EntityDecl, AccessModifier, AbstractModifier, 
		  								       FinalModifier, CyclicTypeReference, Superclasses, 
		  								       Superinterfaces 
		  								       )
  
  def check(oldclz: ClassInfo, newclz: ClassInfo): Option[Problem] = 
    checkRules(rules)(oldclz,newclz) 
}