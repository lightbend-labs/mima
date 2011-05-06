trait A {
  def foo(a: Int): Int
}

class B extends A {
  def foo(a: Int) = 2
}
