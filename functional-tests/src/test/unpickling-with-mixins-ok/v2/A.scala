package foo

trait Alpha {
  def bar(x: Int): Int = x
}

class Beta extends Alpha {
  private[foo] def bar = ""
}
