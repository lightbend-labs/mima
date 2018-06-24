trait T {
  def bar: Unit
}

abstract class A extends T {
  def foo = 1
}
