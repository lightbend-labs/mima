package com.typesafe.tools.mima.core

import scala.annotation.tailrec

class Signature(private val signature: String) {
  import Signature._

  lazy val canonicalized = {
    signature.headOption match {
      case None | Some('(') => signature
      case _ =>
        val (formalTypeParameters, rest) = FormalTypeParameter.parseList(signature.drop(1))
        val replacements = formalTypeParameters.map(_.identifier).zipWithIndex
        replacements.foldLeft(signature) { case (sig, (from, to)) =>
          sig
            .replace(s"<${from}:", s"<__${to}__:")
            .replace(s";${from}:", s";__${to}__:")
            .replace(s"T${from};", s"__${to}__") }
    }
  }

  def matches(newer: Signature, isConstructor: Boolean): Boolean = {
    return (signature == newer.signature) ||
      (isConstructor && hasMatchingCtorSig(newer.signature)) ||
      canonicalized == newer.canonicalized
  }
 
  // Special case for scala#7975
  private def hasMatchingCtorSig(newer: String): Boolean =
    newer.isEmpty || // ignore losing signature on constructors
    signature.endsWith(newer.tail) // ignore losing the 1st (outer) param (.tail drops the leading '(')

    // a method that takes no parameters and returns Object can have no signature
    override def toString = if (signature.isEmpty) "<missing>" else signature
}

object Signature {
  def apply(signature: String): Signature = new Signature(signature)

  val none = Signature("")

  case class FormalTypeParameter(identifier: String, bound: String)
  object FormalTypeParameter {
    def parseList(in: String, listSoFar: List[FormalTypeParameter] = Nil): (List[FormalTypeParameter], String) = {
      in(0) match {
        case '>' => (listSoFar, in.drop(1))
        case o => {
          val (next, rest) = parseOne(in)
          parseList(rest, listSoFar :+ next)
        }
      }
    }
    def parseOne(in: String): (FormalTypeParameter, String) = {
      val identifier = in.takeWhile(_ != ':')
      val boundAndRest = in.dropWhile(_ != ':').drop(1)
      val (bound, rest) = splitBoundAndRest(boundAndRest)
      (FormalTypeParameter(identifier, bound), rest)
    }
    @tailrec
    private def splitBoundAndRest(in: String, boundSoFar: String = "", depth: Int = 0): (String, String) = {
      if (depth > 0) {
        in(0) match {
          case '>' => splitBoundAndRest(in.drop(1), boundSoFar + '>', depth - 1)
          case '<' => splitBoundAndRest(in.drop(1), boundSoFar + '<', depth + 1)
          case o => splitBoundAndRest(in.drop(1), boundSoFar + o, depth)
        }
      } else {
        in(0) match {
          case '<' => splitBoundAndRest(in.drop(1), boundSoFar + '<', depth + 1)
          case ';' => (boundSoFar, in.drop(1))
          case o => splitBoundAndRest(in.drop(1), boundSoFar + o, depth)
        }
      }
    }
  }
}