package com.typesafe.tools.mima.lib

import scala.util.{ Failure, Success, Try }

object TestPrinter {
  def pass = s"${Console.GREEN}\u2713${Console.RESET}" // check mark (green)
  def fail = s"${Console.RED}\u2717${Console.RESET}"   // cross mark (red)

  def testAll[A](xs: Seq[A])(toText: A => String)(test: A => Try[Unit]): Unit = {
    xs.iterator.map(x => run1(toText(x), () => test(x))).foldLeft(Try(())) {
      case (Success(()), res)               => res
      case (res, Success(()))               => res
      case (res @ Failure(e1), Failure(e2)) => e1.addSuppressed(e2); res
    }.get
  }

  def run1(label: String, action: () => Try[Unit]): Try[Unit] = {
    action() match {
      case res @ Success(()) => println(s"+ $pass  $label"); res
      case res @ Failure(ex) => println(s"- $fail  $label: $ex"); res
    }
  }

  if (System.out != Console.out) {
    System.out.println(" System.out identity ##: " + System.identityHashCode(System.out))
    System.out.println("Console.out identity ##: " + System.identityHashCode(scala.Console.out))
    System.out.println("cwd: " + new java.io.File("").getAbsoluteFile)
  }
}
