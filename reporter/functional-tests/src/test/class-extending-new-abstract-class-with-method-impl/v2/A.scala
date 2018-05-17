abstract class A {
  def foo: Int
}

abstract class AA extends A {
  def foo: Int = 1
}

class B extends AA {

}
