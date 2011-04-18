package images

import scala.swing._
import Swing._

object Icons {

  private lazy val sep = java.io.File.separator
  
  private def getIcon = buildIcon(sep + "images" + sep) _ 
  
  /** Load the icon from the given path, or EmptyIcon if not found. */
  private def buildIcon(basePath: String)(filename: String) : javax.swing.Icon = {
    val path = basePath + filename
    getClass.getResource(path) match {
      case null => EmptyIcon 
      case value => 
        val resource = Swing.Icon(value)
        if(resource eq null) EmptyIcon else resource
    }
  }
  
  lazy val install = getIcon("install.png")
  lazy val migration = getIcon("migration.jpg")
  lazy val check = getIcon("check.jpg")
  
  lazy val add = getIcon("add.png")
  lazy val remove = getIcon("remove.gif")
  lazy val up = getIcon("up.gif")
  lazy val down = getIcon("down.gif")
}