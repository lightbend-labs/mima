trait A {
  def foo(a: Int) = println(s"A.foo $a")
}
trait AA extends A {
  override def foo(a: Int) = {
    if (a > 0) {
      println(s"AA.foo $a")
    } else {
      super.foo(a)
    }
  }
}
trait B extends AA
