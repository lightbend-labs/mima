object App {
  def main(args: Array[String]): Unit = {
    val result: String = new OptionPane(("foo", "bar")).show
    println("result: " + result)
  }
}
