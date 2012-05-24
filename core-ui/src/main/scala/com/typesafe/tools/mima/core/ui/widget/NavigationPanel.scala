package com.typesafe.tools.mima.core.ui.widget

import scala.swing._
import event._

/** Simple navigation panel that exposes its three buttons: next, back and exit*/
class NavigationPanel extends BoxPanel(Orientation.Horizontal) {
 
  val back = new Button("Back")
  val next = new Button("Next")
  val exit = new Button("Quit")
  
  contents += Swing.HGlue
  contents += (back, next, Swing.HStrut(20), exit)
  
}