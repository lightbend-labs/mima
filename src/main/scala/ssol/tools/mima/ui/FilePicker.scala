package ssol.tools.mima.ui

import java.io.File

import scala.swing._
import Swing._

import FileChooser._
import FileChooser.Result._
import javax.swing.filechooser.FileNameExtensionFilter

/** UI for choosing a File. It displays the name of the current file 
 *  and a button for showing the FileChooser dialog.
 * 
 */
class FilePicker(_label: String, owner: Component) extends FlowPanel(FlowPanel.Alignment.Left)() {
	private var _selectedFile: Option[File] = None
	
//	border = Swing.EtchedBorder

	def selectedFile: Option[File] = _selectedFile
	
	private val label = new Label(_label) {
		preferredSize = (100, 20)
	}
	private val fileName = new Label("Please select a file")
	
	contents += (label, Swing.HStrut(10))
	contents += Button("Choose") {
		val d = new ClassPathFileChooser
		d.showOpenDialog(owner) match {
			case Approve => 
				fileName.text = d.selectedFile.getName
				_selectedFile = Some(d.selectedFile)
			case _ =>
		}
	}
	
	contents += (fileName, Swing.HGlue)
}
