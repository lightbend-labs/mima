package ssol.tools.mima.ui.wizard

trait WizardAction {
  def isNextEnabled = true
  def isBackEnabled = true
  
  def onNext(): Unit = ()
  def onBack(): Unit = ()
}