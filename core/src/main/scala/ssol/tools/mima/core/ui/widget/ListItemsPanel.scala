package ssol.tools.mima.core.ui.widget

import scala.swing._
import scala.swing.event.ButtonClicked

abstract class ListItemsPanel extends BoxPanel(Orientation.Vertical) {
  
  private val MaxNumberOfItems = 4

  type Item <: Component

  private val add = new Button {
    icon = images.Icons.add
  }

  listenTo(add)
  reactions += {
    case ButtonClicked(`add`) =>
      addConstraint()
  }

  private abstract class Row(val elem: Item) extends FlowPanel(FlowPanel.Alignment.Left)() {
    vGap = 0
    contents += elem

    val remove = new Button {
      icon = images.Icons.remove
    }
    listenTo(remove)
    reactions += {
      case ButtonClicked(`remove`) =>
        removeConstraint(this)
    }

    contents += (add, remove)
  }

  private val view = new BoxPanel(Orientation.Vertical) {
    def +=(r: Row) {
      contents += r
      updateView()
    }

    def -=(r: Row) {
      contents -= r
      updateView()
    }
  }

  contents += view

  final protected def addConstraint() {
    val newRow = new Row(create()) { 
      remove.visible = false
    }
    if(view.contents.nonEmpty) {
      view.contents.last.asInstanceOf[Row].remove.visible = true
    }
    view += newRow
  }

  private def removeConstraint(r: Row) {
    remove(r.elem)
    view -= r
    view.contents.last.asInstanceOf[Row].remove.visible = false
  }
  
  

  private def updateView() {
    add.enabled = (view.contents.size < MaxNumberOfItems) 
    
    repaint()
    revalidate()
  }

  protected def create(): Item

  protected def remove(c: Item): Unit
}
