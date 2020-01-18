trait A
trait B extends A

object Usage {
  def useAfoo(a: A) = 1
  def useAbar(a: A) = 2
  def useBfoo(b: B) = 3
  def useBbar(b: B) = 4
}
