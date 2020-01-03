object App {
  def main(args: Array[String]): Unit = {
    val a = new A
    println(a.fld)
    println(a.foo(a))
    println(a.bar)
    println(new a.Nested)
  }
}
