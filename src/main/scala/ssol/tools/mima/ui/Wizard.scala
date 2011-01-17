package ssol.tools.mima.ui

import scala.swing._

class Wizard extends BorderPanel {
	import BorderPanel._
	
	val buttonsPanel = new BoxPanel(Orientation.Horizontal)
	
	val backButton = Button("Back") { 
	}
	val nextButton = new Button("Next")
	val exitButton = new Button("Exit")
	
	val centerPane = new FlowPanel {
		preferredSize = new Dimension(300, 300)
	}
	
	buttonsPanel.contents += (backButton, nextButton, exitButton)
	add(centerPane, Position.Center)
	add(buttonsPanel, Position.South)
}