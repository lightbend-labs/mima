package ssol.tools.mima
package ui

import javax.swing.UIManager
import java.io.File
import wizard._

import scala.tools.nsc.util.JavaClassPath
import scala.swing._
import Swing._

object MimaLibUI extends SimpleSwingApplication {
	import Swing._
	
//	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
	
	var initialClassPath: JavaClassPath = _
	
	private var residualArgs: List[String] = Nil  
	
	override def startup(args: Array[String]) {
	  Config.setup("scala ssol.tools.misco.MiMaLibUI <old-dir> <new-dir>", args, { (xs) => residualArgs = xs; true }, "-fixall")
	  initialClassPath = Config.classpath
	  super.startup(args)
	}

	// pick up the initial file, if any
  lazy val configurationPage = {
    val (f1, f2) = residualArgs match {
      case List(f1, f2) => (Some(new File(f1)), Some(new File(f2)))
      case _ => (None, None)
    }
    new ConfigurationPanel(initialClassPath, f1, f2)
  }
  
  lazy val reportPage = new ReportPage
  
	def top = new MainFrame() {	  
		title = "Migration Manager Client"
		location = (300, 250)
		preferredSize = (1000, 500)
		
	  val wizard = new Wizard {
			pages += configurationPage
			pages += reportPage
			
			switchTo(0)
		}
		
		contents = wizard
		listenTo(wizard)
		
		reactions += {
		  case Next(`reportPage`) =>
		    println("Reporting now")
		    // set the new settings
		    Config.classpath = configurationPage.classPath
		    val mima = new MiMaLib
		    reportPage.doCompare(configurationPage.oldFile.getAbsolutePath, configurationPage.newFile.getAbsolutePath, mima)
		    
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
