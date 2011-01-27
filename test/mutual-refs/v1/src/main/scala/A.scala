package mutualrefs

class A {
}


class B extends A {
  def foo[T](x: T)(y: T) = y
}
