package foo

class Foo {
  private[foo] val bar: Int = 1
}

object Lib {
  val foo  = new Foo
  def doIt = foo.bar
}
