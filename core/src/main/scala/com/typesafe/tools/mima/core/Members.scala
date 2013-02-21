package com.typesafe.tools.mima.core

import collection.mutable
import collection.TraversableOnce

class Members(val members: TraversableOnce[MemberInfo]) {

  private val bindings = new mutable.HashMap[String, List[MemberInfo]] {
    override def default(key: String) = List()
  }
  for (m <- members) bindings += m.name -> (m :: bindings(m.name))

  def iterator: Iterator[MemberInfo] =
    for (ms <- bindings.valuesIterator; m <- ms.iterator) yield m
  def get(name: String): Iterator[MemberInfo] = bindings(name).iterator
}


object NoMembers extends Members(Nil)