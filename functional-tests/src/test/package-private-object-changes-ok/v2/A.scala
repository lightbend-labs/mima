package foo

private[foo] object Foo {
  def bar(x: Int, y: Int) = x + y
}

object Lib {
  def doIt = Foo.bar(1, 0)
}
