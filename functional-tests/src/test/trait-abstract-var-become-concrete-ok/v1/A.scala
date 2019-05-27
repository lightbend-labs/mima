trait A {
  var foo: Int
}

abstract class B extends A {
  override var foo = 2
}
