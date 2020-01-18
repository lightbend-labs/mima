object App {
  def main(args: Array[String]): Unit = {
    println(A.fld)
    println(A.foo(new A))
    println(A.bar)
    println(new A.Nested)
  }
}
