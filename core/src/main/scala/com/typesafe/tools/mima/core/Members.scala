package com.typesafe.tools.mima.core

import scala.collection.mutable

class Members(members: Iterable[MemberInfo]) {
  private val bindings = new mutable.HashMap[String, List[MemberInfo]]().withDefaultValue(Nil)

  locally {
    for (m <- members)
      bindings += m.bytecodeName -> (m :: bindings(m.bytecodeName))
  }

  def iterator: Iterator[MemberInfo] = for (ms <- bindings.valuesIterator; m <- ms.iterator) yield m

  def get(name: String): Iterator[MemberInfo] = bindings(name).iterator
}

object NoMembers extends Members(Nil)
