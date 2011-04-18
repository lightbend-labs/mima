package ssol.tools.mima.ui.wizard

import scala.collection.mutable

import scala.swing._
import scala.swing.event._
import Swing._
import ssol.tools.mima.ui.Exit

object Wizard {
  object WizardExit extends Enumeration {
    type WizardExit = Value
    val Begin, End = Value
  }
  case class WizardExit(exit: WizardExit.WizardExit) extends event.Event

  object LoadingPanel extends FlowPanel {
    contents += new Label("Loading...")
  }
}

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
  import Wizard._

  /** The current wizard pages. */
  protected val pages: mutable.Buffer[WizardPanel] = new mutable.ArrayBuffer[WizardPanel]

  /** Switch to the given wizard page number. */
  private def switchTo(page: Int) {
    val panel = pages(page)
    centerPane.swap(panel)
    revalidate()
    repaint()
  }

  def start() = {
    assert(pages.size > 0, "Empty Wizard cannot be started")
    switchTo(0)
  }

  private val backButton = new Button("Back")
  private val nextButton = new Button("Next")
  private val exitButton = new Button("Quit")

  // the main area where wizard pages are displayed
  private val centerPane = new BorderPanel {
    private def showLoadingPanel() {
      _contents.clear()
      _contents += LoadingPanel
      buttonsPanel.visible = false
      revalidate()
    }

    def swap(page: WizardPanel) {
      showLoadingPanel()
      
      val worker = new SwingWorker {
        def act() = {
          page.beforeDisplay()
          showPage(page)
        }
      }

      worker.start()
    }

    private def showPage(page: WizardPanel) {
      _contents.clear()
      _contents += page
      buttonsPanel.visible = true
      revalidate()
    }
  }

  private def currentPage = _currentPage
  private var _currentPage = 0

  // the bottom section where the navigation buttons are
  private val buttonsBox = new BoxPanel(Orientation.Horizontal) {
    contents += Swing.HGlue
    contents += (backButton, nextButton, Swing.HStrut(20), exitButton)
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
      if (currentPage + 1 < pages.length) {
        _currentPage += 1
        switchTo(currentPage)
      } else publish(WizardExit(WizardExit.End))

    case ButtonClicked(`backButton`) =>
      val panel = pages(currentPage)
      if (_currentPage > 0) {
        _currentPage -= 1
        switchTo(currentPage)
      } else publish(WizardExit(WizardExit.Begin))

    case ButtonClicked(`exitButton`) =>
      publish(Exit)
  }
}

