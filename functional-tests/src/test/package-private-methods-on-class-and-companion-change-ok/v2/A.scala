package foo

class Foo {
  private[foo] def bar(x: Int, y: Int) = x + y
  private[foo] def qux(x: Int, y: Int) = x + y
}

object Foo {
  private[foo] def baz(x: Int, y: Int) = x + y
  private[foo] def qux(x: Int, y: Int) = x + y
}

object Lib {
  val foo  = new Foo
  def doIt = foo.bar(1, 0) + Foo.baz(1, 0) + foo.qux(1, 0) + Foo.qux(1, 0)
}
