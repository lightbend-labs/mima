package com.typesafe.tools.mima.lib

import scala.util.{ Failure, Success, Try }

import munit.{ GenericTest, Location }

object Test {
  def apply(label: String, action: => Try[Unit]): Test1 = Test1(label, () => action)

  def pass = s"${Console.GREEN}\u2713${Console.RESET}" // check mark (green)
  def fail = s"${Console.RED}\u2717${Console.RESET}"   // cross mark (red)

  def testAll(tests: List[Test]): Try[Unit] = {
    tests.iterator.map(_.run()).foldLeft(Try(())) {
      case (res @ Failure(e1), Failure(e2)) => e1.addSuppressed(e2); res
      case (res @ Failure(_), _)            => res
      case (_, res)                         => res
    }
  }

  def run1(label: String, action: () => Try[Unit]): Try[Unit] = {
    action() match {
      case res @ Success(()) => println(s"+ $pass  $label"); res
      case res @ Failure(ex) => println(s"- $fail  $label: $ex"); res
    }
  }

  implicit class TestOps(private val t: Test) extends AnyVal {
    def tests: List[Test1] = t match {
      case t1: Test1    => List(t1)
      case Tests(tests) => tests
    }

    def munitTests: List[GenericTest[Unit]] = for {
      test <- t.tests
    } yield new GenericTest(test.label, () => test.unsafeRunTest(), Set.empty, Location.empty)
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

case class Test1(label: String, action: () => Try[Unit]) extends Test
case class Tests(tests: List[Test1])                     extends Test
