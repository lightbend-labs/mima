package ssol.tools.mima.ui.wizard

import scala.swing.Component

/*
object WizardPage {
  object CanGoNext extends event.Event
}*/

trait WizardPage extends Component {
  /** data model for the page */
    val data = collection.mutable.Map.empty[String, Any]
    
    /** is called before the page is displayed. Long-running task should be 
     * executed here (a loading panel is automatically displayed).*/
    def onLoad() {}
    
    /** is called whenever the page was not visible on screen and becomes visible. 
     * */
    def onReveal() {}
    
    /** is called whenever the page was visible on screen and is being hidden. */
    def onHide() {}
    
    /** is called before switching to the next page. */
    def onNext() {}
    
    /** is called before switching to the previous page. */
    def onBack() {}
  
  
  /*
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
  */
}