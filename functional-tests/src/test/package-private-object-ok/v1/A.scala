package foo

class C {
  private[foo] class K {
    def f = 1
  }
  private[foo] object U {
    def f = 1
  }
  def f = (new K).f + U.f
}
