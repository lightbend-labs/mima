trait DialogSource[+A] { def show(): A }

final class OptionPane(val source: (javax.swing.JOptionPane, String)) extends DialogSource[Any] {
  def show(): Any = {
    val (pane, title) = source
    val jdlg = pane.createDialog(title)
    jdlg.setVisible(true)
    pane.getValue
  }
}
