package ssol.tools.mima.ui.wizard

import scala.collection.mutable

import scala.swing._
import scala.swing.event._
import Swing._
import ssol.tools.mima.ui.Exit

/** A simple wizard interface. It consist of a center panel that displays
 *  the current page. There are three buttons for navigating forward, back
 *  and for canceling the wizard.
 *  
 *  Example:
 *  
 *  {{{
 *    val wiz = new Wizard {
 *      pages += new Button("Page 1")
 *      pages += new Label("Page 2")
 *    }
 *  }}
 *  
 *  This class publishes two events. 
 *  @see PageChanged, Cancelled
 */
class Wizard extends BorderPanel {
	import BorderPanel._
	
	private type WizardPanel = Component with WizardAction
	
	/** The current wizard pages. */
	protected val pages: mutable.Buffer[WizardPanel] = new mutable.ArrayBuffer[WizardPanel]

	/** Switch to the given wizard page number. */
	private def switchTo(page: Int) {
		centerPane.swap(pages(page))
		updateButtons()
		revalidate()
		repaint()
	}
	
	def start() = {
	  assert (pages.size > 0 , "Empty Wizard cannot be started")
	  switchTo(0)
	}
	
	private def updateButtons() {
	  val currentPanel = pages(currentPage)
	  nextButton.enabled = currentPanel.isNextEnabled
	  backButton.enabled = currentPanel.isBackEnabled
	}
		
	private val backButton = new Button("Back")
	private val nextButton = new Button("Next")
	private val exitButton = new Button("Quit")
	
	// the main area where wizard pages are displayed
	private val centerPane = new BorderPanel {
		preferredSize = (500, 300)
		
		def swap(page: Component) {
			_contents.clear()
			_contents += pages(currentPage)
			revalidate()
		}
	}
	
	private def currentPage = _currentPage
	private var _currentPage = 0
	
	// the bottom section where the navigation buttons are
	private val buttonsBox = new BoxPanel(Orientation.Horizontal) {
		contents += Swing.HGlue
		contents += (backButton, nextButton, Swing.HStrut(20), exitButton, Swing.HStrut(10))
	}

	private val buttonsPanel = new BorderPanel {
		add(new Separator, Position.North)
		add(buttonsBox, Position.South)
	}
	
	add(centerPane, Position.Center)
	add(buttonsPanel, Position.South)
	
	listenTo(backButton, nextButton, exitButton)
	
	reactions += {
		case ButtonClicked(`nextButton`) =>
		  assert (currentPage + 1 < pages.length)
		  val panel = pages(currentPage)
		  panel.onNext() 
		  _currentPage += 1
		  switchTo(currentPage)

		case ButtonClicked(`backButton`) =>
		  val panel = pages(currentPage)
		  panel.onBack() 
		  if(_currentPage > 0) {
			  _currentPage -= 1
			  switchTo(currentPage)
		  }
		  
		case ButtonClicked(`exitButton`) =>
		  publish(Exit)
	}
}

