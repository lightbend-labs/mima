package ssol.tools.mima.core.ui.widget

import java.awt.Graphics;
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File

import scala.swing._  
import Swing._

import ssol.tools.mima.core.util.Browse

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

import scala.swing.event.MouseClicked

class LinkImagePanel(val uri: String, img: javax.swing.Icon) extends ImagePanel(img){
  require(uri != null)
  
  cursor = new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)
    listenTo(mouse.clicks)
    reactions += {
      case e: MouseClicked => Browse to uri
    }
}