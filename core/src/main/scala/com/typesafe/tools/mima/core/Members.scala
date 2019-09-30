package com.typesafe.tools.mima.core

sealed class Members[+A <: MemberInfo](val value: List[A]) {
  final val dict: Map[String, List[A]]     = value.groupBy(_.bytecodeName)
  final def get(name: String): Iterator[A] = dict.getOrElse(name, Nil).iterator
}

object NoMembers extends Members(Nil)
