package ssol.tools.mima.lib.ui.page

import java.io.File

import scala.swing._
import ssol.tools.mima.core.Config

import Swing._
import GridBagPanel._

import ssol.tools.mima.core.ui.widget.{ FilePicker, ClassPathEditor }
import ssol.tools.mima.core.ui.widget.FilePicker.FileChanged
import ssol.tools.mima.core.ui.WithConstraints

object ConfigurationPanel {
  case class FilesSelected(oldLib: File, newLib: File) extends event.Event
}

/**
 * A Panel used to configure MiMa. It allows jar file selection
 *  and setting up the classpath.
 */
class ConfigurationPanel(f1: Option[File] = None, f2: Option[File] = None) extends Component {

  override lazy val peer: javax.swing.JComponent = componee.peer

  private lazy val componee = new GridBagPanel with WithConstraints {

    private val oldFilePickerLabel = new Label("Old:") { preferredSize = (40, preferredSize.height) }
    private val newFilePickerLabel = new Label("New:") { preferredSize = (40, preferredSize.height) }

    val oldFilePicker = new FilePicker(oldFilePickerLabel, this, f1)
    val newFilePicker = new FilePicker(newFilePickerLabel, this, f2)

    listenTo(oldFilePicker)
    listenTo(newFilePicker)

    reactions += {
      case FileChanged(_, _) =>
        if (areFilesSelected)
          notifyFilesSelected()
    }

    val ins = new Insets(0, 0, 10, 0)

    import java.awt.Color
    val files = new GridPanel(3, 1) {
      border = LineBorder(Color.lightGray)
      contents += new Label("Select the two libraries' versions you would like to compare") {
        border = EmptyBorder(0, 5, 0, 0)
        horizontalAlignment = Alignment.Left
      }
      contents += oldFilePicker
      contents += newFilePicker
    }

    withConstraints(gridx = 0, gridy = 0, weightx = 1, fill = Fill.Both, anchor = Anchor.PageEnd, insets = ins) {
      add(files, _)
    }

    val cpEditor = new ClassPathEditor(Nil) {
      classpathLabel.text = "Library classpath:"
    }

    withConstraints(gridx = 0, gridy = 1, weightx = 1, weighty = 1, fill = Fill.Both, anchor = Anchor.PageEnd) {
      add(cpEditor, _)
    }
  }

  private def oldFile = componee.oldFilePicker.selectedFile
  private def newFile = componee.newFilePicker.selectedFile

  private def notifyFilesSelected() {
    publish(ConfigurationPanel.FilesSelected(oldFile.get, newFile.get))
  }

  def areFilesSelected: Boolean = oldFile.isDefined && newFile.isDefined

  def cpEditor = componee.cpEditor
}