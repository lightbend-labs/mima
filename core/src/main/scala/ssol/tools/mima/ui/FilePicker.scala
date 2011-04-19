package ssol.tools.mima.ui

import java.io.File

import scala.swing._
import Swing._
import ssol.tools.mima.Config

import FileChooser._
import FileChooser.Result._
import javax.swing.filechooser.FileNameExtensionFilter

object FilePicker {
  private var lastSelectedFile: Option[File] = None
  
}

/** UI for choosing a File. It displays the name of the current file 
 *  and a button for showing the FileChooser dialog.
 */
class FilePicker(_label: String, owner: Component, private var _selectedFile: Option[File]) extends FlowPanel(FlowPanel.Alignment.Left)() {
  
	def selectedFile: Option[File] = _selectedFile
	private def selectedFile_=(file: File) = {
	  _selectedFile = Some(file)
	  FilePicker.lastSelectedFile = _selectedFile
	}
	
  private val fileNameLabel = new Label("Please select a file") {
    
  }

	if (selectedFile.isDefined) {
	  Config.debugLog("starting with initial file: " + selectedFile.get.getAbsolutePath)
	  fileNameLabel.text = selectedFile.get.getName
	}
	
	private val label = new Label(_label)
	
	contents += (label, Swing.HStrut(10))
	contents += Button("Choose") {
		val d = new ClassPathFileChooser(FilePicker.lastSelectedFile.map(_.getParentFile).getOrElse(null))
		d.showOpenDialog(owner) match {
			case Approve => 
			  val fileName = d.selectedFile.getName
			  fileNameLabel.text  = if(fileName.length > 40) "[...]"+d.selectedFile.getName.takeRight(35) else fileName
				selectedFile = d.selectedFile
				publish(FileSelected(this, d.selectedFile))
			case _ =>
		}
	}
	
	contents += (fileNameLabel, Swing.HGlue)
}

case class FileSelected(source: FilePicker, file: File) extends event.Event