package ssol.tools.mima.ui

import scala.swing._
import event._


class NavigationPanel extends BoxPanel(Orientation.Horizontal) {
 
  val backButton = new Button("Back")
  val nextButton = new Button("Next")
  val exitButton = new Button("Quit")
  
  contents += Swing.HGlue
  contents += (backButton, nextButton, Swing.HStrut(20), exitButton)
  
}