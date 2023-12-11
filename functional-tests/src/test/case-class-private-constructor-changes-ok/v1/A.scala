package foo

case class Foo private (a: String)

object Foo {
  def apply() = new Foo("a")

  private def unapply(f: Foo) = f
}
