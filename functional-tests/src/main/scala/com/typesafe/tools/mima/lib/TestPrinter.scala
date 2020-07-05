package com.typesafe.tools.mima.lib

import scala.util.{ Failure, Success, Try }

object TestPrinter {
  def pass = s"${Console.GREEN}\u2713${Console.RESET}" // check mark (green)
  def fail = s"${Console.RED}\u2717${Console.RESET}"   // cross mark (red)

  def ok(msg: String)                          = Success(println(msg))
  def ko(msg: String, cause: Throwable = null) = Failure(new Exception(msg, cause))

  def testAll[A](xs: Seq[A])(toText: A => String)(test: A => Try[Unit]): Unit = {
    xs.foldLeft(Try(())) {
      case (f: Failure[_], _) => f
      case (_, x)             => test(x) match {
        case Success(()) => ok(s"+ $pass  ${toText(x)}")
        case Failure(ex) => ko(s"- $fail  ${toText(x)}", ex)
      }
    }.get

  if (System.out != Console.out) {
    System.out.println(" System.out identity ##: " + System.identityHashCode(System.out))
    System.out.println("Console.out identity ##: " + System.identityHashCode(scala.Console.out))
    System.out.println("cwd: " + new java.io.File("").getAbsoluteFile)
  }
}
