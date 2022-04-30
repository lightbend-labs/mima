object App {
  def main(args: Array[String]): Unit = {
    val (s, i)                = new A().foo
    val (ss: String, ii: Int) = (s, i)
    println(s + "bar")
    println(i + 1)
    println(ss + "bar")
    println(ii + 1)
  }
}
