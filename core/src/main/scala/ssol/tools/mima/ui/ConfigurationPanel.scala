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
class ConfigurationPanel(f1: Option[File] = None, f2: Option[File] = None) extends GridBagPanel with WithConstraints {
  
  private val oldFilePicker = new FilePicker("Old: ", this, f1)
  private val newFilePicker = new FilePicker("New:", this, f2)
  
  def oldFile = oldFilePicker.selectedFile.get
  def newFile = newFilePicker.selectedFile.get
  
  def areFilesSelected: Boolean = oldFilePicker.selectedFile.isDefined && newFilePicker.selectedFile.isDefined
  
  listenTo(oldFilePicker)
  listenTo(newFilePicker)
  
  reactions += {
    case FileSelected(_,_) =>
      if(areFilesSelected) publish(FilesSelected(oldFile, newFile))
  }
  
  val ins = new Insets(0, 10, 10, 10)
  
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
  
  withConstraints(gridx = 0, gridy = 1, weightx = 1, weighty = 1, fill = Fill.Both, anchor = Anchor.PageEnd, insets = ins) {
    add(cpEditor, _)
  }
}

case class FilesSelected(oldLib: File, newLib: File) extends event.Event  