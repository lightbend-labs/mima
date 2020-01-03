trait A {
  def foo(): Unit
}

object Usage {
  def use(a: A) = a.foo()
}
