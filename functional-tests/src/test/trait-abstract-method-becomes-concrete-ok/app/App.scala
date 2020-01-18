object App {
  def main(args: Array[String]): Unit = {
    println(new A { def foo = 2 }.foo)
    println(new B().foo)
    println(new B { override def foo = 3 }.foo)
  }
}
