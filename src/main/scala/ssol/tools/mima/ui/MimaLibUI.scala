package ssol.tools.mima.ui

import javax.swing.UIManager
import wizard._

import scala.swing._
import Swing._

object MimaLibUI extends SimpleSwingApplication {
	import Swing._
	
	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
	
	val top = new MainFrame() {	  
		title = "Migration Manager Client"
		location = (200, 200)
		
	  val wizard = new Wizard {
			pages += new ConfigurationPanel
			
			pages += new BoxPanel(Orientation.Horizontal) {
				contents += new Label("I am the walrus")
			}
			
			switchTo(0)
		}
		
		contents = wizard
		listenTo(wizard)
		
		reactions += {
			case Cancelled() =>
			  Dialog.showConfirmation(parent = wizard, 
			  		title = "Exit Mimalib", 
			  		message = "Are you sure you want to quit?") match {
			  	case Dialog.Result.Ok => exit(0)
			  	case _ => ()
			  }
		}
	}
}
