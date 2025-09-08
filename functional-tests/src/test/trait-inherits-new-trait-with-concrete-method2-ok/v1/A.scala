trait A {
  def foo(a: Int) = println(s"A.foo $a")
}
trait B extends A
