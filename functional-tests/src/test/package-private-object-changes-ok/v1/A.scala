package foo

private[foo] object Foo {
  def bar(x: Int) = x
}

object Lib {
  def doIt = Foo.bar(1)
}
