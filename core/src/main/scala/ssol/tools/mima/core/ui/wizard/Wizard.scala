package ssol.tools.mima.core.ui.wizard

import scala.collection.mutable

import scala.actors.Actor

import scala.swing._
import scala.swing.event._
import Swing._

import ssol.tools.mima.core.ui.event.ExitMiMa
import ssol.tools.mima.core.ui.widget.NavigationPanel

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
  private[Wizard] object WizardPageProxy {
    def apply(_page: => WizardPage) = new WizardPageProxy { override def page = _page }
  }
  private[Wizard] abstract class WizardPageProxy {
    private var cached: Option[WizardPage] = None 
    def clear() = cached = None
    protected def page: WizardPage
    def get = cached match {
      case None =>
        cached = Some(page)
        cached.get
      case Some(page) => page
    }
  }
  
  private class LoadingPanel extends BorderPanel {
    private val loading = new ssol.tools.mima.core.ui.widget.LoadingPanel("")
    private val wrapper = Component.wrap(loading)
    add(wrapper, BorderPanel.Position.Center)
    add(ssol.tools.mima.core.util.log.UiLogger, BorderPanel.Position.South)
    
    def start() = loading.start()
  }

  import BorderPanel._

  /** The current wizard pages. */
  private val pages: mutable.Buffer[WizardPageProxy] = new mutable.ArrayBuffer[WizardPageProxy]

  def +=(_page: => WizardPage) = pages += WizardPageProxy(_page)

  /** Switch to the given wizard page number. */
  private def switchTo(page: Int) {
    _currentPage = page

    val current = pages(_currentPage).get

    centerPane.swap(current)
    updateNavigationButtons()

    // explicitly redraw the container
    revalidate()
    repaint()
  }

  private def updateNavigationButtons() {
    navigation.next.enabled = pages(_currentPage).get.canNavigateForward
    navigation.next.visible = currentIsNotLastPage
    navigation.back.visible = currentIsNotFirstPage && pages(_currentPage).get.canNavigateBack
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

    val page = pages(currentPage).get
    val nextPage = pages(currentPage + 1).get
    page.onNext()
    notifyHide(page)
    nextPage.model ++= page.model

    switchTo(currentPage + 1)
  }

  private def back() {
    checkStarted
    assert(currentPage - 1 >= 0)

    val page = pages(currentPage).get
    val previousPage = pages(currentPage - 1).get
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
      import ssol.tools.mima.core.ui.widget.BugReportDialog
      new Actor {
        def act() = {
          try {
        	  task
          } catch {
            case t: Throwable =>
              new BugReportDialog(t)()
          }
          Swing onEDT { onEDT }
        }
      }
    }

    private def showLoadingPanel() {
      buttonsPanel.visible = false
      val loading = new LoadingPanel()
      setContent(loading)
      // delaying start so that `setContent` will trigger 
      // component's `repaint` which will update the loading 
      // panel's size. The size is needed to correctly center 
      // the loading animation.
      Swing onEDT { loading.start() }
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

  navigation.exit.action = Action("Quit") { publish(ExitMiMa) }

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