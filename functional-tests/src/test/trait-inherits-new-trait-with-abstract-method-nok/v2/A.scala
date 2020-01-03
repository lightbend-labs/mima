trait AA {
  def foo: Int
  val bar = 2
}
trait A extends AA
trait B extends A

object Usage {
  def useAfoo(a: A) = a.foo + 1
  def useAbar(a: A) = a.bar + 2
  def useBfoo(b: B) = b.foo + 3
  def useBbar(b: B) = b.bar + 4
}
