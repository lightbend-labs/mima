trait A {
  def foo: Int
}

trait B extends A

object Usage {
  def use(a: A): Int = a.foo
}
