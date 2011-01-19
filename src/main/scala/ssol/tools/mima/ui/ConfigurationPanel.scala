package ssol.tools.mima.ui

import scala.swing._

import java.io.File

import Swing._
import GridBagPanel._

/** A Panel used to configure MiMa. It allows jar file selection
 *  and setting up the classpath.
 * 
 */
class ConfigurationPanel extends GridBagPanel {
	val oldFilePicker = new FilePicker("Old version:", this)
	val newFilePicker = new FilePicker("New version:", this)

	val c = new Constraints
	c.fill = Fill.Horizontal
	
//	border = LineBorder(java.awt.Color.RED)
	
	c.gridx = 0
	c.gridy = 0
	c.anchor = Anchor.PageEnd
	c.weightx = 1.0
	c.fill = Fill.Both
	
	
	c.insets = new Insets(10, 10, 10, 10)

	import java.awt.Color
	val files = new GridPanel(2, 1) {
	  border = LineBorder(Color.lightGray)
	  contents += oldFilePicker
	  contents += newFilePicker
	}
	layout(files) = c
	
//	layout(oldFilePicker) = c
//	c.gridy = 1
//	layout(newFilePicker) = c

	c.gridy = 2
  layout(new Separator) = c

  c.gridy = 3
  c.fill = Fill.Both
  c.weighty = 1.0
  val cp = new ClassPathEditor
  layout(cp) = c
  
  
//  val bottom = new Panel {
//		
//		border = LineBorder(java.awt.Color.RED)
//	}
//	c.gridy = 3
//	c.fill = Fill.Both
//	c.weighty = 1.0
//	layout(bottom) = c
}