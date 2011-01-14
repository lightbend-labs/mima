package ssol.tools.mima.ui

import scala.swing._

class Wizard extends Panel {
	val buttonsPanel = new BoxPanel(Orientation.Horizontal)
	
	val backButton = Button("Back") { 
	}
	
	buttonsPanel += backButton
}