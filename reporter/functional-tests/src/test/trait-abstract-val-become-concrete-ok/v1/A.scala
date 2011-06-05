trait A {
  val foo: Int
}

abstract class B extends A {
  override val foo = 2
}
