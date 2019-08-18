package bar
class A {
  def foo[T](x: T): T = ???
  private[bar] def foo[T](x: T, y: T): T = ???
}
