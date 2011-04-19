package ssol.tools.mima.ui.wizard

import scala.swing._

abstract class WizardPage {
  val content: Component
  
  def beforeDisplay() = ()
  def onNext() = ()
  def onBack() = ()
}