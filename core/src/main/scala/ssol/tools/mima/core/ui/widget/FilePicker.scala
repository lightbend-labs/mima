package ssol.tools.mima.core.ui.widget

import java.io.File

import scala.swing._
import Swing._
import ssol.tools.mima.core.Config

import FileChooser._
import FileChooser.Result._
import javax.swing.filechooser.FileNameExtensionFilter

object FilePicker {
  /**
   * Keeps a reference to the last selected file.
   * XXX: This works as a cache. We may want to move this into `Prefs`
   */
  private var lastSelectedFile: Option[File] = None

  /** Event triggered upon selection of a file*/
  case class FileChanged(source: FilePicker, file: File) extends event.Event
}

/**
 * Jar File Picker. It displays the name of the current file
 *  and a button for selecting a different jar file.
 *
 *  @event: A `FileChanged` event is published every time the selected file is changed.
 */
class FilePicker(private val label: Label, owner: Component, private var _selectedFile: Option[File]) extends Component {

  import FilePicker._

  override lazy val peer = componee.peer

  private lazy val componee = new FlowPanel(FlowPanel.Alignment.Left)() {

    private def selectedFile_=(file: File) = {
      /** Shorten file name at the somewhat arbitrary size of 40 chars*/
      def shortName(file: File): String = {
        val MaxFileNameLength = 40
        if (file.getName.length > MaxFileNameLength)
          "..." + file.getName.takeRight(35)
        else file.getName
      }

      _selectedFile = Some(file)
      lastSelectedFile = _selectedFile
      fileName.text = shortName(file)
      notifiyFileSelectedChanged()
    }

    private val fileName = new Label("Please select a file")

    if (_selectedFile.isDefined)
      selectedFile_=(_selectedFile.get)

    // position UI elements
    contents += (label, Swing.HStrut(10))

    contents += Button("Choose") {
      val d = new JarFileChooser(lastSelectedFile.map(_.getParentFile).getOrElse(null))
      d.showOpenDialog(owner) match {
        case Approve =>
          selectedFile_=(d.selectedFile)
        case _ =>
      }
    }

    contents += (fileName, Swing.HGlue)
  }

  private def notifiyFileSelectedChanged() {
    publish(FileChanged(FilePicker.this, selectedFile.get))
  }
  
  def selectedFile: Option[File] = _selectedFile
}