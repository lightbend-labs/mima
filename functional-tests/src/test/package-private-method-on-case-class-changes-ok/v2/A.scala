package foo

case class Foo() {
  private[foo] def bar(x: Int, y: Int) = x + y
}

object Lib {
  val foo  = new Foo
  def doIt = foo.bar(1, 0)
}
