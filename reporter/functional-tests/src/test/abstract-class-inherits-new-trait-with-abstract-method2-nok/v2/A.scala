trait AA {
  def foo: Int
}
trait A extends AA
trait B extends A
abstract class C extends B
