object App {
  def main(args: Array[String]): Unit =
    println(new ClassAnalyzer().superclasses)
}
