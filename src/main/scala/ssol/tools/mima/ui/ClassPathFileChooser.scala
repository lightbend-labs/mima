package ssol.tools.mima.ui

import javax.swing.filechooser._

import scala.swing._
import FileChooser._

/** A file chooser that filters files to be jars, and
 *  allows selecting a directory as well.
 */
class ClassPathFileChooser extends FileChooser{
  fileSelectionMode = SelectionMode.FilesAndDirectories
  fileFilter = new FileNameExtensionFilter("Jar files", "jar")
}