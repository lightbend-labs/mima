object App {
  def main(args: Array[String]): Unit = {
    println(new B().foo)
    println(new A { def foo = 1 }.foo)
    println(new A { override def foo = 1 }.foo)
  }
}
