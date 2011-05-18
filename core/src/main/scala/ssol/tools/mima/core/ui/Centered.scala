package ssol.tools.mima.core.ui

import scala.swing.Frame
import scala.swing.Swing._

import java.awt.Toolkit
import java.awt.Dimension

trait Centered { self: Frame =>
  def center = {
    val tk = Toolkit.getDefaultToolkit
    val screenSize = tk.getScreenSize
    val screenHeight = screenSize.height
    val screenWidth = screenSize.width
    ((screenWidth - preferredSize.width) / 2,
      (screenHeight - preferredSize.height) / 2)
  }

  def centerFrame = location = center
}