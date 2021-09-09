package foo

package x {
  trait T {
    def somethingSoThatTHasInitMethod = 0
  }
}

package y {
  class C extends x.T {
    def f = 0
  }
}
