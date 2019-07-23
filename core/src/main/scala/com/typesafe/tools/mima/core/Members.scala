package com.typesafe.tools.mima.core

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

sealed class Members[+A <: MemberInfo](val value: List[A]) {
  val dict = {
    val initial = value.foldLeft(mutable.Map.empty[String, ListBuffer[A]]) { (bindings, m) =>
      bindings.getOrElseUpdate(m.bytecodeName, new ListBuffer[A]) += m
      bindings
    }
    initial.foldLeft(Map.empty[String, List[A]]) { case (bindings, (bytecodeName, members)) =>
      bindings.updated(bytecodeName, members.result())
    }
  }

  def iterator: Iterator[A]          = value.iterator
  def get(name: String): Iterator[A] = dict.getOrElse(name, Nil).iterator
}

object NoMembers extends Members(Nil)
