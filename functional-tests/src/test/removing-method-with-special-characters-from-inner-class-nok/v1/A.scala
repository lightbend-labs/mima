package foo

// See https://github.com/lightbend/mima/issues/158
// `<` would be filtered out, therefore not reporting the missing method
object A {
  abstract class < {
    def f: Int
  }
  abstract class B {
    def f: Int
  }

  def giveLt: < = new < { def f = 1 }
  def giveB: B  = new B { def f = 2 }
}
