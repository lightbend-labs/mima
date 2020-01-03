object App {
  def main(args: Array[String]): Unit = {
    val boxed = new Bar().boxedStringy
    val unboxed = boxed.get
    val string: String = boxed.get
    println(boxed)
    println(unboxed)
    println(string)
  }
}
