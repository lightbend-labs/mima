trait AA {
  def foo: Int
}
abstract class A extends AA
class B extends A {
  override def foo: Int = 2
}
