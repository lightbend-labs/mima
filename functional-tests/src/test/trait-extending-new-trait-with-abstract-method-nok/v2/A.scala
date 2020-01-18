trait A extends B
trait B {
  val a: Int
  def foo: Int
}

object Usage {
  def use(a: A) = a.a + a.foo + 1
}
