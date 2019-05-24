trait A {
  val foo = 1
}

abstract class B extends A {
  override val foo = 2
}
