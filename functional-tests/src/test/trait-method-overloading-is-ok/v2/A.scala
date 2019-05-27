trait A {
  def foo(a: Int) = a
  def foo(a: Object): Int = foo(a.asInstanceOf[Int])
}