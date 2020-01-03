object App {
  def main(args: Array[String]): Unit = {
    println(new A {}.foo)
    println(new B().foo)
    println(new A { override def foo = 10 }.foo)
    println(new B { override def foo = 11 }.foo)
  }
}
