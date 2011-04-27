package ssol.tools.mima.ui.page


import scala.swing._
import Swing._

import GridBagPanel._

import java.io.File

import ssol.tools.mima.ui.WithConstraints
import ssol.tools.mima.ui.widget.{FileSelected, ClassPathEditor, FilePicker}

class ProjectConfigurationPage(f1: Option[File] = None) extends GridBagPanel with WithConstraints {

  private val projectFilePicker = new FilePicker("Project jar: ", this, f1)
  
  def projectFile = projectFilePicker.selectedFile.get
  
  def isProjectFileSelected = projectFilePicker.selectedFile.isDefined
  
  listenTo(projectFilePicker)
  
  reactions += {
    case FileSelected(_,_) => publish(ProjectSelected(projectFile))
  }
  
  val ins = new Insets(0, 0, 10, 0)
  
  import java.awt.Color
  val files = new GridPanel(2, 1) {
    border = LineBorder(Color.lightGray)
    contents += new Label("Select the project jar that you would like to migrate") {
      border = EmptyBorder(0, 5, 0, 0)
      horizontalAlignment = Alignment.Left
    }
    contents += projectFilePicker
  }
  
  
  withConstraints(gridx = 0, gridy = 0, weightx = 1, fill = Fill.Both, anchor = Anchor.PageEnd, insets = ins) {
    add(files, _)
  }
  
  val cpEditor = new ClassPathEditor(Nil) {
    classpathLabel.text = "Project classpath:"
  }
  
  withConstraints(gridx = 0, gridy = 1, weightx = 1, weighty = 1, fill = Fill.Both, anchor = Anchor.PageEnd) {
    add(cpEditor, _)
  }
}

case class ProjectSelected(file: File) extends event.Event