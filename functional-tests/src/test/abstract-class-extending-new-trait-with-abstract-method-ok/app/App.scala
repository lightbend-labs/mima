object App {
  def main(args: Array[String]): Unit = {
    object a extends A { def foo() = () }
    println(a.foo())
  }
}
