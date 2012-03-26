package ssol.tools.mima.core.util.log

import scala.swing.{Component, Label}
import scala.swing.Swing

import ssol.tools.mima.core.Config

object UiLogger extends Component with Logging {
  
  private lazy val label = new Label
  
  override lazy val peer = label.peer
  
	def info(str: String): Unit = Swing.onEDT { label.text = str } 
  def debugLog(str: String): Unit = if(Config.debug) Swing.onEDT { label.text = str } 
}