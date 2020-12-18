abstract class Super {
  def bar: Number
}
class Foo extends Super {
  override def bar: java.lang.Long = 42L
}
