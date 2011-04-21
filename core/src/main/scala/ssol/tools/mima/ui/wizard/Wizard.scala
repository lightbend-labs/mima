package ssol.tools.mima.ui.wizard

import scala.collection.mutable

import scala.swing._
import scala.swing.event._
import Swing._
import ssol.tools.mima.ui.Exit
import scala.actors.Actor

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
  private object LoadingPanel extends FlowPanel {
    contents += new Label("Loading...")
  }

  import BorderPanel._

  /** The current wizard pages. */
  private val pages: mutable.Buffer[WizardPage] = new mutable.ArrayBuffer[WizardPage]

  def +=(page: WizardPage) = pages += page
  def ++=(pages: Seq[WizardPage]) = pages.foreach(+=(_))

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

  def next() {
    val page = pages(currentPage)
    page.onNext()
    notifyHide(page)
    if (currentPage + 1 < pages.length) {
      _currentPage += 1
      switchTo(currentPage)
    }
  }

  def back() {
    val page = pages(currentPage)
    page.onBack()
    notifyHide(page)
    val panel = pages(currentPage)
    if (_currentPage > 0) {
      _currentPage -= 1
      switchTo(currentPage)
    }
  }

  // the main area where wizard pages are displayed
  private val centerPane = new BorderPanel {
    def swap(page: WizardPage) {
      showLoadingPanel()

      val worker = new Actor {
        def act() = {
          page.onLoad()
          // use swing-event-thread for ui modifications
          Swing onEDT {
            hideLoadingPanel()
            setContent(page)
            //buttonsBox.nextButton.enabled = page.isForwardNavigationEnabled
            //buttonsBox.backButton.enabled = page.isBackwardNavigationEnabled
          }
        }
      }

      worker.start()
      notifyReveal(page)
    }

    private def showLoadingPanel() {
      buttonsPanel.visible = false
      setContent(LoadingPanel)
    }

    private def hideLoadingPanel() {
      buttonsPanel.visible = true
    }

    private def setContent(content: Component) {
      _contents.clear()
      _contents += content
      revalidate()
    }
  }

  private def currentPage = _currentPage
  private var _currentPage = 0

  import ssol.tools.mima.ui.NavigationPanel
  // the bottom section where the navigation buttons are
  private val buttonsBox = new NavigationPanel

  private val buttonsPanel = new BorderPanel {
    add(new Separator, Position.North)
    add(buttonsBox, Position.South)
  }

  add(centerPane, Position.Center)
  add(buttonsPanel, Position.South)

  buttonsBox.nextButton.action = Action("Next") { next() }

  buttonsBox.backButton.action = Action("Back") { back() }

  buttonsBox.exitButton.action = Action("Quit") { publish(Exit) }

  private def notifyReveal(page: WizardPage) = {
    page.onReveal()
  }

  private def notifyHide(page: WizardPage) = {
    page.onHide()
  }
}

