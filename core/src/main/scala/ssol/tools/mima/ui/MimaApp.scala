package ssol.tools.mima
package ui

import javax.swing.UIManager
import java.io.File

import scala.tools.nsc.util.JavaClassPath
import scala.swing._
import Swing._

object MimaApp extends SimpleSwingApplication {

  var resargs: List[String] = Nil 
  
  override def startup(args: Array[String]) {
    resargs = Config.setup("scala ssol.tools.misco.MiMaLibUI <old-dir> <new-dir>", args, "-fixall")
    super.startup(args)
  }

  def top = new MimaFrame
}
