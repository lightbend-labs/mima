package foo

class Foo {
  private[foo] def bar(x: Int) = x
}

object Lib {
  val foo  = new Foo
  def doIt = foo.bar(1)
}
