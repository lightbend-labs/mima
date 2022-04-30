trait A

trait B extends A {
  def foo: Int
}

object Usage {
  def use(a: A): Int = 0
}
