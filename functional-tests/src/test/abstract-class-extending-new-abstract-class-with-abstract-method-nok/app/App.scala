object App {
  def main(args: Array[String]): Unit = {
    println(new A {}.foo(new A {}))
    println(new C)
    println(new D)

    println(new E { def bar(): E = new E { def bar() = this } }.bar())
    println(new F)
  }
}
