trait A {
  def foo = 2
}

class B extends A {
  override def foo: Int = 3
}