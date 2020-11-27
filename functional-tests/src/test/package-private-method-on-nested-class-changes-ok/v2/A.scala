package foo

class Foo {
  class Bar {
    private[foo] def bar(x: Int, y: Int) = x + y
  }
}

object Lib {
  val foo  = new Foo
  val bar  = new foo.Bar
  def doIt = bar.bar(1, 0)
}
