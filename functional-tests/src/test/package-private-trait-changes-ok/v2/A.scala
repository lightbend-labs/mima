package foo

private[foo] trait Foo {
  def bar(x: Int, y: Int) = x + y
}

object Lib {
  val foo  = new Foo {}
  def doIt = foo.bar(1, 0)
}
