package ssol.tools.mima.ui.wizard

import scala.swing._

object WizardPage {
  object CanGoNext extends event.Event
}

abstract class WizardPage extends Publisher {
  /** The displayed content of this page*/
  val content: Component

  
  val isBackwardNavigationEnabled = true
  
  private var _isForwardNavigationEnabled = true

  def isForwardNavigationEnabled = _isForwardNavigationEnabled
  def isForwardNavigationEnabled_=(enabled: Boolean) {
    if (_isForwardNavigationEnabled == enabled) return
    _isForwardNavigationEnabled = enabled
    if (_isForwardNavigationEnabled)
      publish(WizardPage.CanGoNext)
  }
  
  /** Put here validation code that will allow/block forward navigation*/
  def canNavigateForward: () => Boolean = () => true

  /** Use this to execute tasks that do not affect the UI.
   *  Mind to schedule ui code in the Swing event dispatching
   *  thread (EDT).
   */
  def beforeDisplay() = ()

  def onEntering() = ()

  def onLeaving() = ()

  /** Put here code that has to be executed before moving to the next page.
   *  Mind that it uses the swing event thread.
   */
  def onNext() = ()

  /** Put here code that has to be executed before moving to the previous page.
   *  Mind that it uses the swing event thread.
   */
  def onBack() = ()
}