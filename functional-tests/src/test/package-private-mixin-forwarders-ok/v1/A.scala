package foo

package x {
  trait T {
    def somethingSoThatTHasInitMethod = 0
    private[foo] def bar(x: Int) = x
  }
}

package y {
  class C extends x.T {
    // uncommenting this makes the test pass
    // override private[foo] def bar(x: Int) = x
    def f = bar(0)
  }
}
