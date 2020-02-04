abstract class Super {
  def bar: Number
}
class Foo extends Super {
  override def bar = 42
}
