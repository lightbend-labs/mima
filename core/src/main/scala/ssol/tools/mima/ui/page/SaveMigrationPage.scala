package ssol.tools.mima.ui.page

import scala.swing._
import event._
import Swing._
import GridBagPanel._
import java.awt.GridBagConstraints._

import ssol.tools.mima.ui.WithConstraints

class SaveMigrationPage extends GridBagPanel with WithConstraints {

  private val topText = new TextArea("Please, select a target directory for the migration. " +
    "You should specify a qualifier to name each migrated jar file (i.e., migrated jars " +
    "are named <original-jar-name>-<qualifier>.jar)") {
    editable = false
    opaque = false
    lineWrap = true
    wordWrap = true
  }

  val ins = new Insets(10, 10, 10, 10)

  withConstraints(weightx = 1, fill = Fill.Horizontal, insets = ins) {
    add(topText, _)
  }

  protected val selectDirectory = new GridBagPanel with WithConstraints {
    import java.io.File

    private var _selected: File = _

    def selected_=(selected: File) = {
      _selected = selected
      dir.text = selected.getAbsolutePath
    }

    def selected: File = _selected

    private def chooseDirectory(dir: File) = new FileChooser(dir) {
      fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
    }

    withConstraints(gridx = 0, insets = new Insets(5, 0, 0, 5)) {
      add(new Label("Target directory:"), _)
    }

    private val dir = new TextField {
      focusable = false
    }

    withConstraints(gridx = 1, weightx = 1, fill = Fill.Horizontal) {
      add(dir, _)
    }

    listenTo(dir.mouse.clicks)
    reactions += {
      case e: MouseClicked =>
        val dirPicker = chooseDirectory(selected)
        dirPicker.showOpenDialog(dir) match {
          case FileChooser.Result.Approve =>
            selected = dirPicker.selectedFile
          case _ => ()
        }
    }
  }

  withConstraints(gridx = REMAINDER, gridy = 1, fill = Fill.Horizontal, weightx = 1, insets = ins) {
    add(selectDirectory, _)
  }

  protected val qualifierPanel = new GridBagPanel with WithConstraints {

    private val qualifierLabel = new Label("Qualifier:")

    withConstraints(insets = new Insets(5, 0, 0, 5)) {
      add(qualifierLabel, _)
    }

    val qualifier = new TextField("migrated")

    withConstraints(weightx = 1, fill = Fill.Horizontal) {
      add(qualifier, _)
    }
  }

  withConstraints(gridy = 2, insets = ins, fill = Fill.Horizontal, weightx = 1) {
    add(qualifierPanel, _)
  }

  withConstraints(gridy = 3, fill = Fill.Both, weightx = 1, weighty = 1) {
    add(Glue, _)
  }
}