package ssol.tools.mima.core.ui.widget

import scala.swing._
import scala.swing.event.ButtonClicked

abstract class ListItemsPanel extends BoxPanel(Orientation.Vertical) {

  type Item <: Component
  
  private class Row(val elem: Item) extends FlowPanel(FlowPanel.Alignment.Left)() {
    vGap = 0
    private val add = new Button {
      icon = images.Icons.add
    }
    private val remove = new Button {
      icon = images.Icons.remove
    }
    listenTo(add, remove)

    reactions += {
      case ButtonClicked(`add`) =>
        addConstraint()

      case ButtonClicked(`remove`) =>
        removeConstraint(this)
    }

    contents ++= Seq(elem, add)
    if (view.contents.size > 0)
      contents += remove
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
    view += new Row(create())
  }

  private def removeConstraint(r: Row) {
    remove(r.elem)
    view -= r
  }

  private def updateView() {
    repaint()
    revalidate()
  }

  protected def create(): Item

  protected def remove(c: Item): Unit
}
