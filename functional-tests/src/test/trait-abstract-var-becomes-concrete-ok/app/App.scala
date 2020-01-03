object App {
  def main(args: Array[String]): Unit = {
    val a = new A { var foo = 1 }
    println(a.foo)
    a.foo = 10
    println(a.foo)

    val b = new B {}
    println(b.foo)
    b.foo = 11
    println(b.foo)

    val ba: A = new B {}
    println(ba.foo)
    ba.foo = 11
    println(ba.foo)
  }
}
