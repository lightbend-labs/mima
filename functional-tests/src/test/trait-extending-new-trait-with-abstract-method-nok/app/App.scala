object App {
  def main(args: Array[String]): Unit =
    println(Usage.use(new A {}) + 10)
}
