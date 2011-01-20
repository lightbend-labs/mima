package ssol.tools.mima
package ui

import javax.swing.UIManager
import wizard._

import scala.tools.nsc.util.JavaClassPath

import scala.swing._
import Swing._

object MimaLibUI extends SimpleSwingApplication {
	import Swing._
	
//	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
	
	var initialClassPath: JavaClassPath = _
	
	override def startup(args: Array[String]) {
	  Config.setup("scala ssol.tools.misco.MiMaLibUI <old-dir> <new-dir>", Array(), (xs) => true, "-fixall")
	  initialClassPath = new PathResolver(Config.settings).mimaResult
	  super.startup(args)
	}

  lazy val configurationPage = new ConfigurationPanel(initialClassPath) 
  
	def top = new MainFrame() {	  
		title = "Migration Manager Client"
		location = (300, 250)
		preferredSize = (1000, 500)
		
	  val wizard = new Wizard {
			pages += configurationPage
			
			pages += new BoxPanel(Orientation.Horizontal) {
				contents += new Label("I am the walrus")
			}
			
			switchTo(0)
		}
		
		contents = wizard
		listenTo(wizard)
		
		reactions += {
		  case PageChanged(_, _) =>
		    println("new classpath: " + configurationPage.classPath)
		  
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
