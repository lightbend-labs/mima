object App {
  def main(args: Array[String]): Unit =
    println(new some.A().foo[App.type](App)(App))
}
