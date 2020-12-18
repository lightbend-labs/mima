package com.typesafe.tools.mima.lib

import scala.util.{ Failure, Success, Try }

import munit.{ GenericTest, Location }

object Test {
  def apply(label: String, action: => Try[Unit]): Test1 = Test1(label, () => action)

  def pass = s"${Console.GREEN}\u2713${Console.RESET}" // check mark (green)
  def fail = s"${Console.RED}\u2717${Console.RESET}"   // cross mark (red)

  def testAll(tests: List[Test1]): Try[Unit] = {
    val (successes, failures) = tests.map(t => t -> Test.run1(t.label, t.action)).partition(_._2.isSuccess)
    println(s"${tests.size} tests, ${successes.size} successes, ${failures.size} failures")
    if (failures.nonEmpty) {
      val failureNames = failures.map { case ((t1, _)) => t1.name }
      println("Failures:")
      failureNames.foreach(name => println(s"* $name"))
      println(s"functional-tests/Test/run ${failureNames.mkString(" ")}")
    }
    failures.foldLeft(Try(())) {
      case (res @ Failure(e1), (_, Failure(e2))) => e1.addSuppressed(e2); res
      case (res @ Failure(_), _)                 => res
      case (_, (_, res))                         => res
    }
  }

  def run1(label: String, action: () => Try[Unit]): Try[Unit] = {
    action() match {
      case res @ Success(()) => println(s"+ $pass  $label"); res
      case res @ Failure(ex) => println(s"- $fail  $label: $ex"); res
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

object Test1 {
  implicit class Ops(private val t: Test1) extends AnyVal {
    def name: String = t.label.indexOf(" / ") match {
      case -1  => t.label
      case idx => t.label.drop(idx + 3)
    }
  }
}

object Tests {
  implicit class Ops(private val t: Tests) extends AnyVal {
    def munitTests: List[GenericTest[Unit]] = for {
      test <- t.tests
    } yield new GenericTest(test.label, () => test.unsafeRunTest(), Set.empty, Location.empty)
  }
}

case class Test1(label: String, action: () => Try[Unit]) extends Test
case class Tests(tests: List[Test1])                     extends Test
