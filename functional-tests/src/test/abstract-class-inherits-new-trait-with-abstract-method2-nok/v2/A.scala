trait AA {
  def foo: Int
}
trait A extends AA
trait B extends A
abstract class C extends B

object Usage {
  def useA(a: A) = a.foo + 1
  def useB(b: B) = b.foo + 2
  def useC(c: C) = c.foo + 3
}
