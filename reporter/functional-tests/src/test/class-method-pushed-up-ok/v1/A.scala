class A {
  def foo(x: Int) = x
}

class B extends A {
  override def foo(x: Int) = super.foo(2)
}
