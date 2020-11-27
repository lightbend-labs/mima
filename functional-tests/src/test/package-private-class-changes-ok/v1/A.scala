package foo

private[foo] class Foo {
  def bar(x: Int) = x
}

object Lib {
  val foo  = new Foo
  def doIt = foo.bar(1)
}
