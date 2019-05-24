trait AA {
  def foo: Int
  val bar = 2
}
trait A extends AA
trait B extends A
