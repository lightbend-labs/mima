package foo

class Foo {
  class Bar {
    private[foo] def bar(x: Int) = x
  }
}

object Lib {
  val foo  = new Foo
  val bar  = new foo.Bar
  def doIt = bar.bar(1)
}
