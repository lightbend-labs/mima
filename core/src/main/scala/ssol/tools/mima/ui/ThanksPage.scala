package ssol.tools.mima.ui

import scala.swing._
import event._

class ThanksPage extends GridBagPanel {

  val thanksLabel = new Label("Congratulations, your classes have been migrated.")
  
  // position elements in GridBagPanel
  
  import GridBagPanel._
  import java.awt.GridBagConstraints._

  private val c = new Constraints
  
  layout(thanksLabel) = c
}