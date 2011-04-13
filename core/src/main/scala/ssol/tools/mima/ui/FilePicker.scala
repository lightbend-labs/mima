package ssol.tools.mima.ui

import java.io.File

import scala.swing._
import Swing._
import ssol.tools.mima.Config

import FileChooser._
import FileChooser.Result._
import javax.swing.filechooser.FileNameExtensionFilter

/** UI for choosing a File. It displays the name of the current file 
 *  and a button for showing the FileChooser dialog.
 * 
 */
class FilePicker(_label: String, owner: Component, private var _selectedFile: Option[File]) extends FlowPanel(FlowPanel.Alignment.Left)() {
	
//	border = Swing.EtchedBorder

	def selectedFile: Option[File] = _selectedFile
  private val fileNameLabel = new Label("Please select a file")

	if (selectedFile.isDefined) {
	  Config.debugLog("starting with initial file: " + selectedFile.get.getAbsolutePath)
	  fileNameLabel.text = selectedFile.get.getName
	}
	
	private val label = new Label(_label) {
		preferredSize = (100, 20)
	}
	
	contents += (label, Swing.HStrut(10))
	contents += Button("Choose") {
		val d = new ClassPathFileChooser(selectedFile.map(_.getParentFile).getOrElse(null))
		d.showOpenDialog(owner) match {
			case Approve => 
				fileNameLabel.text = d.selectedFile.getName
				_selectedFile = Some(d.selectedFile)
			case _ =>
		}
	}
	
	contents += (fileNameLabel, Swing.HGlue)
}
