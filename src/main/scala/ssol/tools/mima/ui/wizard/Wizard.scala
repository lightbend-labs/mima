package ssol.tools.mima.ui.wizard

import scala.collection.mutable

import scala.swing._
import scala.swing.event._
import Swing._

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

	/** The current wizard pages. */
	val pages: mutable.Buffer[Panel] = new mutable.ArrayBuffer[Panel]

	/** Switch to the given wizard page number. */
	def switchTo(page: Int) {
		centerPane.swap(pages(page))
		revalidate()
		repaint()
	}
		
	private val backButton = new Button("Back")
	private val nextButton = new Button("Next")
	private val exitButton = new Button("Exit")
	
	// the main area where wizard pages are displayed
	val centerPane = new BorderPanel {
		preferredSize = (500, 300)
		
		def swap(page: Component) {
			_contents.clear()
			_contents += pages(currentPage)
			revalidate()
		}

	}
	
	def currentPage = _currentPage
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
		  if (currentPage + 1 < pages.length) _currentPage += 1
		  switchTo(currentPage)
		  publish(PageChanged(pages(currentPage - 1), pages(currentPage)))
      publish(Next(pages(currentPage)))

		case ButtonClicked(`backButton`) =>
		  if (currentPage > 0) _currentPage -= 1
		  switchTo(currentPage)
		  publish(PageChanged(pages(currentPage + 1), pages(currentPage)))
		  publish(Back(pages(currentPage)))
		  
		case ButtonClicked(`exitButton`) =>
		  publish(Cancelled())
	}
}

case class Back(newPage: Panel) extends Event
case class Next(newPage: Panel) extends Event
case class PageChanged(oldPage: Panel, newPage: Panel) extends Event
case class Cancelled() extends Event
