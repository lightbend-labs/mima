package ssol.tools.mima.core.ui

import javax.swing.UIManager
import java.io.File

import scala.swing._
import Swing._
import ssol.tools.mima.core.Config
import ssol.tools.mima.core.util.WithEventQueueProxy

trait MimaSwingApp extends SimpleSwingApplication with WithEventQueueProxy {

  var resargs: List[String] = Nil 
  
  override def startup(args: Array[String]) {
    resargs = Config.setup("scala " + launcherClassName +" <old-dir> <new-dir>", args, "-fixall")
    super.startup(args)
  }

  protected def launcherClassName: String
  
  
  override def top: Frame
}
