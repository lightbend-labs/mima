package ssol.tools.mima.ui.wizard

import scala.collection.mutable

import scala.actors.Actor

import scala.swing._
import scala.swing.event._
import Swing._

import ssol.tools.mima.ui.Exit
import ssol.tools.mima.ui.widget.NavigationPanel

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
    _currentPage = page

    val content = pages(_currentPage)

    centerPane.swap(content)
    updateNavigationButtons()

    // explicitly redraw the container
    revalidate()
    repaint()
  }

  private def updateNavigationButtons() {
    navigation.next.enabled = pages(_currentPage).canNavigateForward
    navigation.next.visible = currentIsNotLastPage
    navigation.back.visible = currentIsNotFirstPage
  }

  private def currentIsNotLastPage: Boolean =
    _currentPage < pages.length - 1

  private def currentIsNotFirstPage: Boolean =
    _currentPage > 0

  private var started = false

  def start() = {
    assert(!started, "Wizard cannot be started twice")
    assert(pages.size > 0, "Empty Wizard cannot be started")

    started = true
    switchTo(0)
  }

  private def next() {
    checkStarted
    assert(currentPage + 1 < pages.length)

    val page = pages(currentPage)
    val nextPage = pages(currentPage + 1)
    page.onNext()
    notifyHide(page)
    nextPage.model ++= page.model

    switchTo(currentPage + 1)
  }

  private def back() {
    checkStarted
    assert(currentPage - 1 >= 0)

    val page = pages(currentPage)
    val previousPage = pages(currentPage - 1)
    page.onBack()
    notifyHide(page)
    page.model.clear

    switchTo(currentPage - 1)
  }

  private def checkStarted {
    assert(started, "Wizard was not started.")
  }

  // the main area where wizard pages are displayed
  private val centerPane = new BorderPanel {
    def swap(page: WizardPage) {
      showLoadingPanel()

      worker(page.onLoad()) {
        hideLoadingPanel()
        setContent(page)
        notifyReveal(page)
      }.start()
    }

    private def worker(task: => Unit)(onEDT: => Unit): Actor = {
      new Actor {
        def act() = {
          task
          Swing onEDT { onEDT }
        }
      }
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
  
  // the bottom section where the navigation buttons are
  private val navigation = new NavigationPanel

  private val buttonsPanel = new BorderPanel {
    add(new Separator, Position.North)
    add(navigation, Position.South)
  }

  add(centerPane, Position.Center)
  add(buttonsPanel, Position.South)

  navigation.next.action = Action("Next") { next() }

  navigation.back.action = Action("Back") { back() }

  navigation.exit.action = Action("Quit") { publish(Exit) }

  private def notifyReveal(page: WizardPage) = {
    listenTo(page)
    page.onReveal()
  }

  private def notifyHide(page: WizardPage) = {
    deafTo(page)
    page.onHide()
  }

  reactions += {
    case WizardPage.CanGoNext(allowed) => navigation.next.enabled = allowed
  }
}

