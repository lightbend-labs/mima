package ssol.tools.mima.ui.wizard

import scala.swing._

trait WizardPanel extends Component {
  def beforeDisplay(): Unit = ()
  
  def onNext(): Unit = ()
}