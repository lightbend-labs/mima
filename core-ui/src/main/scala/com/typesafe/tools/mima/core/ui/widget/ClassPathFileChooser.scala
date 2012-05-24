package com.typesafe.tools.mima.core.ui.widget

import javax.swing.filechooser._
import java.io.File

import scala.swing._
import FileChooser._

/** A file chooser that filters files to be jars, and
 *  allows selecting a directory as well.
 */
class ClassPathFileChooser(dir: File) extends FileChooser(dir) {
  def this() = this(null)

  multiSelectionEnabled = true
  peer.setAcceptAllFileFilterUsed(false)
  fileSelectionMode = SelectionMode.FilesOnly
  fileFilter = new FileNameExtensionFilter("Jar files", "jar")
}

class JarFileChooser(dir: File) extends ClassPathFileChooser(dir) {
  multiSelectionEnabled = false
}