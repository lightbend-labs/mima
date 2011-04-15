package ssol.tools.mima.ui

import java.awt.Graphics;
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File

import scala.swing._  
import Swing._

class ImagePanel(img: javax.swing.Icon) extends Panel {
  
  preferredSize = (img.getIconWidth, img.getIconHeight)
  
  def this(url: java.net.URL) = this(Icon(url))
  def this(path: String) = this(Icon(getClass.getResource(path)))
  def this(image: java.awt.Image) = this(Icon(image))
  
  override def paintComponent(g: Graphics2D) {
    super.paintComponent(g)
    img.paintIcon(peer, g, 0, 0)
  }
}