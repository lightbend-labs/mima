object App {
  def main(args: Array[String]): Unit = {
    println(new B().foo(3))
    println(new A().foo(4))
    println(new A { override def foo(x: Int) = super.foo(x * 10) }.foo(5))
  }
}
