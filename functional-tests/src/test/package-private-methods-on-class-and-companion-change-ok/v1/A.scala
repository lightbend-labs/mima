package foo

class Foo {
  private[foo] def bar(x: Int) = x
  private[foo] def qux(x: Int) = x
}

object Foo {
  private[foo] def baz(x: Int) = x
  private[foo] def qux(x: Int) = x
}

object Lib {
  val foo  = new Foo
  def doIt = foo.bar(1) + Foo.baz(1) + foo.qux(1) + Foo.qux(1)
}
