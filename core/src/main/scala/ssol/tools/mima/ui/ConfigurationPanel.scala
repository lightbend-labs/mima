package ssol.tools.mima
package ui

import java.io.File

import scala.tools.nsc.{ util, io }
import util._
import io.AbstractFile

import scala.swing._
import ssol.tools.mima.Config

import Swing._
import GridBagPanel._

/**
 * A Panel used to configure MiMa. It allows jar file selection
 *  and setting up the classpath.
 */
class ConfigurationPanel(initialClassPath: JavaClassPath, f1: Option[File] = None, f2: Option[File] = None) extends GridBagPanel with wizard.WizardAction {
  val oldFilePicker = new FilePicker("Old version:", this, f1)
  val newFilePicker = new FilePicker("New version:", this, f2)

  val c = new Constraints
  c.fill = Fill.Horizontal

  c.gridx = 0
  c.gridy = 0
  c.anchor = Anchor.PageEnd
  c.weightx = 1.0
  c.fill = Fill.Both

  c.insets = new Insets(10, 10, 10, 10)

  import java.awt.Color
  val files = new GridPanel(2, 1) {
    border = LineBorder(Color.lightGray)
    contents += oldFilePicker
    contents += newFilePicker
  }
  layout(files) = c

  c.gridy = 2
  layout(new Separator) = c

  c.gridy = 3
  c.fill = Fill.Both
  c.weighty = 1.0

  import ClassPath._

  val cpEditor = new ClassPathEditor(split(initialClassPath.asClasspathString))
  layout(cpEditor) = c

  def oldFile = oldFilePicker.selectedFile.get
  def newFile = newFilePicker.selectedFile.get

  def classPath: JavaClassPath = {
    val cpString = cpEditor.classPathString
    Config.debugLog(cpString)
    new JavaClassPath(DefaultJavaContext.classesInPath(cpString), DefaultJavaContext)
  }

}