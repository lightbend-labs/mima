trait A {
  def foo = 2
  val buz = 2
}

class B extends A {
  override def foo = 2
  override val buz = foo
}
