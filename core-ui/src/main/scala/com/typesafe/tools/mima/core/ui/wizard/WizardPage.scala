package com.typesafe.tools.mima.core.ui.wizard

import scala.swing.Component

object WizardPage {
  import scala.swing.event.Event
  /** used to inform `Wizard` whether forward navigation is `allowed`. */
  case class CanGoNext(allowed: Boolean) extends Event
  
  trait Model {
    /** model is backed by a mutable map. */
    protected val data = collection.mutable.Map.empty[String, Any]

    /** copies `that` into `this`. */
    def ++=(that: Model): Unit = data ++= that.data
    def clear(): Unit = data.clear()
  }
}

trait WizardPage extends Component {
  /** data model for the page.
   *  Used as a communication channel between the pages.
   */
  protected[wizard] val model: WizardPage.Model

  /** is forward navigation enabled. */
  def canNavigateForward = true
  
  /** is Back navigation enabled. */
  def canNavigateBack = true

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