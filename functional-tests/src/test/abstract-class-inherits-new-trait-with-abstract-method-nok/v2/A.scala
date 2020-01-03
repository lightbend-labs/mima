trait AA {
  def foo(): Int
}

trait A extends AA
abstract class B extends A

object Usage {
  def useA(a: A) = a.foo() + 1
  def useB(b: B) = b.foo() + 2
}
