package ssol.tools.mima.ui

import scala.swing._

object MimaLibUI extends SimpleSwingApplication {
	val top = new MainFrame() {
		title = "Migration Manager Client"
	  contents = new Wizard
	}
}