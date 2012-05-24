package com.typesafe.tools.mima.core.ui

import javax.swing.UIManager
import java.io.File

import scala.swing._
import Swing._
import com.typesafe.tools.mima.core.Config
import com.typesafe.tools.mima.core.util.WithUncaughtExceptionHandlerDialog

trait MimaSwingApp extends SimpleSwingApplication with WithUncaughtExceptionHandlerDialog {

  var resargs: List[String] = Nil 
  
  override def startup(args: Array[String]) {
    resargs = Config.setup("scala " + launcherClassName +" <old-dir> <new-dir>", args, "-fixall")
    super.startup(args)
  }

  protected def launcherClassName: String
  
  
  override def top: Frame
}
