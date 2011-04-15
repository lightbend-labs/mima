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
	  residualArgs = Config.setup("scala ssol.tools.misco.MiMaLibUI <old-dir> <new-dir>", args, "-fixall")
	  initialClassPath = Config.baseClassPath
	  super.startup(args)
	}

  // pick up the initial file, if any
  lazy val configurationPage = {
    val (f1, f2) = residualArgs match {
      case List(f1, f2) => (Some(new File(f1)), Some(new File(f2)))
      case _            => (None, None)
    }
    new ConfigurationPanel(initialClassPath, f1, f2)
  }

  
	def top = new MimaPanel
}
