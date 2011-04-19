package ssol.tools.mima
package ui

import java.io.File

import scala.swing._
import ssol.tools.mima.Config

import Swing._
import GridBagPanel._

/**
 * A Panel used to configure MiMa. It allows jar file selection
 *  and setting up the classpath.
 */
class ConfigurationPanel(f1: Option[File] = None, f2: Option[File] = None) extends GridBagPanel {
  
  private val oldFilePicker = new FilePicker("Old: ", this, f1)
  private val newFilePicker = new FilePicker("New:", this, f2)
  
  def oldFile = oldFilePicker.selectedFile.get
  def newFile = newFilePicker.selectedFile.get
  
  def areFilesSelected = oldFilePicker.selectedFile.isDefined && newFilePicker.selectedFile.isDefined
  
  listenTo(oldFilePicker)
  listenTo(newFilePicker)
  
  reactions += {
    case FileSelected(_,_) =>
      if(areFilesSelected) publish(FilesSelected(oldFile, newFile))
  }
  
  val c = new Constraints
  c.fill = Fill.Horizontal

  c.gridx = 0
  c.gridy = 0
  c.anchor = Anchor.PageEnd
  c.weightx = 1.0
  c.fill = Fill.Both

  c.insets = new Insets(0, 10, 10, 10)

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
  layout(files) = c

  c.gridy = 1
  c.fill = Fill.Both
  c.weighty = 1.0

  val cpEditor = new ClassPathEditor(Nil) {
    classpathLabel.text = "Library classpath:"
  }
  layout(cpEditor) = c
}

case class FilesSelected(oldLib: File, newLib: File) extends event.Event  