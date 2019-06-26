trait DialogSource[+A] { def show(): A }

final class OptionPane[A](val source: (MyPane[A], String)) extends DialogSource[A] {
  def show(): A = {
    val (pane, title) = source
    val jdlg = pane.peer.createDialog(title)
    jdlg.setVisible(true)
    pane.result
  }
}

trait MyPane[A] {
  def peer: javax.swing.JOptionPane
  def result: A
}
