package ssol.tools.mima.core.ui

import scala.swing._
import scala.swing.event._

import java.net.URI

import ssol.tools.mima.core.ui.event.Exit
import ssol.tools.mima.core.util.Browse

object MimaMenuBar extends MenuBar {
  import ssol.tools.mima.core.util.Urls._
  // File menu
	val file = new Menu("File") {
		mnemonic = Key.F
	}
	
	contents += file
	
	val exit = new MenuItem(new Action("Exit") {
	  def apply() = publish(Exit)
	}) {
	  mnemonic = Key.E
	}
	
	file.contents += exit
	
	
	// File menu
	val help = new Menu("Help") {
		mnemonic = Key.H
	}
	
	contents += help
	
	val guide = new MenuItem(new Action("Online resources") {
	  def apply() =  Browse to OnlineResource
	}) {
	  mnemonic = Key.T
	}
	
	val bug = new MenuItem(new Action("Report a bug...") {
	  def apply() = Browse to BugReporting
	}) {
	  mnemonic = Key.R
	}
	
	help.contents += (guide, bug)
	
	
}