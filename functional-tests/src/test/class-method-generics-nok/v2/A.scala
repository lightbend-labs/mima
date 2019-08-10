class A {
  def genericWithChangingName[T]: Option[T] = ???
  def backwardsCompatibleNarrowing: Option[String] = ???
}

final class Api {
  def cov1[A](): A        = ??? // OK
  def cov2(): Any         = ??? // KO

  def con1[A](x: A): Unit = () // KO
  def con2(x: Any): Unit  = () // OK
}

abstract class Abi {
  def cov1[A](): A // KO
  def cov2(): Any  // OK

  def con1[A](x: A): Unit // OK
  def con2(x: Any): Unit  // KO
}
