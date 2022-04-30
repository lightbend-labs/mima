package com.typesafe.tools.mima.lib

sealed trait Direction {
  def ordered[A](v1: A, v2: A) = this match {
    case Backwards => (v1, v2)
    case Forwards  => (v2, v1)
  }
  def oracleFile = this match {
    case Backwards => "problems.txt"
    case Forwards  => "forwards.txt"
  }
}
case object Backwards extends Direction
case object Forwards extends Direction
