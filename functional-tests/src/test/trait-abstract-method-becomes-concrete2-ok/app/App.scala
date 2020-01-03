object App {
  def main(args: Array[String]): Unit = {
    println(new A1 {})
    val a       = new A { def foo = 2 }
    val aa1: A1 = new A { def foo = 3 }
    println(a.bar)
    println(a.foo)
    println(aa1)
    val b = new B
    val ba: A = new B
    val ba1: A1 = new B
    println(b.bar)
    println(b.foo)
    println(ba.bar)
    println(ba.foo)
    println(ba1)
  }
}
