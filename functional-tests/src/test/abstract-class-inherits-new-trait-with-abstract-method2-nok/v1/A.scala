trait A
trait B
abstract class C extends A with B

object Usage {
  def useA(a: A) = 0
  def useB(b: B) = 0
  def useC(c: C) = 0
}
