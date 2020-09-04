package com.typesafe.tools.mima.lib

import scala.util.{ Failure, Success, Try }

object Test {
  def apply(label: String, action: => Unit): Test1 = Test1(label, () => action)

  def pass = s"${Console.GREEN}\u2713${Console.RESET}" // check mark (green)
  def fail = s"${Console.RED}\u2717${Console.RESET}"   // cross mark (red)

  def testAll(tests: List[Test]): Try[Unit] = {
    tests.iterator.map(_.run()).foldLeft(Try(())) {
      case (res @ Failure(e1), Failure(e2)) => e1.addSuppressed(e2); res
      case (res @ Failure(_), _)            => res
      case (res, _)                         => res
    }
  }

  def run1(label: String, action: () => Unit): Try[Unit] = {
    Try(action()) match {
      case res @ Success(()) => println(s"+ $pass  $label"); res
      case res @ Failure(ex) => println(s"- $fail  $label: $ex"); res
    }
  }

  implicit class TestOps(private val t: Test) extends AnyVal {
    def tests: List[Test1] = t match {
      case t1: Test1    => List(t1)
      case Tests(tests) => tests
    }
  }
}

sealed trait Test {
  def unsafeRunTest(): Unit = run().get

  def run(): Try[Unit] = this match {
    case Test1(l, a) => Test.run1(l, a)
    case Tests(ts)   => Test.testAll(ts)
  }

  override def toString = this match {
    case Test1(label, _) => s"Test($label)"
    case Tests(tests)    => s"Tests(${tests.mkString("[", ", ", "]")})"
  }
}

case class Test1(label: String, action: () => Unit) extends Test
case class Tests(tests: List[Test1])                extends Test
