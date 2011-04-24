package ssol.tools.mima.ui.wizard

import scala.swing.Component

object WizardPage {
  import scala.swing.event.Event
  /** used to inform `Wizard` that forward navigation is allowed. */
  object CanGoNext extends Event
  
  trait Model {
    /** model is backed by a mutable map. */
    protected val data = collection.mutable.Map.empty[String, Any]

    /** copies `that` model into `this`. */
    def ++=(that: Model): Unit = data ++= that.data
    def clear(): Unit = data.clear()
  }
}

trait WizardPage extends Component {
  /** data model for the page.
   *  Used as a communication channel between the pages.
   */
  protected[wizard] val model: WizardPage.Model

  /** enables forward navigation. */
  def canNavigateForward = true

  /** is called before the page is displayed. Long-running task should be
   *  executed here (a loading panel is automatically displayed).
   */
  def onLoad() {}

  /** is called whenever the page was not visible on screen and becomes visible. */
  def onReveal() {}

  /** is called whenever the page was visible on screen and is being hidden. */
  def onHide() {}

  /** is called before switching to the next page. */
  def onNext() {}

  /** is called before switching to the previous page. */
  def onBack() {}
}