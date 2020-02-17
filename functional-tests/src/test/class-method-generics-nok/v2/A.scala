class A {
  def genericWithChangingName[T]: Option[T] = ???
  def backwardsCompatibleNarrowing: Option[String] = ???
}

final class Api {
  def cov1[T](): T        = null.asInstanceOf[T] // OK
  def cov2(): Any         = ()                   // KO

  def con1[T](x: T): Unit = () // KO
  def con2(x: Any): Unit  = () // OK
}

abstract class Abi {
  def cov1[T](): T // KO
  def cov2(): Any  // OK

  def con1[T](x: T): Unit // OK
  def con2(x: Any): Unit  // KO
}
