package com.typesafe.tools.mima.lib

sealed trait Direction {
  def lhs(testCase: TestCase) = this match {
    case Backwards => testCase.outV1
    case Forwards  => testCase.outV2
  }
  def rhs(testCase: TestCase) = this match {
    case Backwards => testCase.outV2
    case Forwards  => testCase.outV1
  }
  def oracleFile = this match {
    case Backwards => "problems.txt"
    case Forwards  => "forwards.txt"
  }
}
case object Backwards extends Direction
case object  Forwards extends Direction
