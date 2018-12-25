package com.typesafe.tools.mima.core.util

import scala.collection.mutable

final class EmptyMutableMap[K, V] private() extends mutable.AbstractMap[K, V]
    with mutable.Map[K, V]
    with mutable.MapLike[K, V, EmptyMutableMap[K, V]]
{
  override def empty = this

  def -=(key: K) = this
  def +=(kv: (K, V)) = this

  def get(key: K) = None
  def iterator = Iterator.empty
}

object EmptyMutableMap {
  private val instance = new EmptyMutableMap[Any, Any]
  def get[K, V]: EmptyMutableMap[K, V] = instance.asInstanceOf[EmptyMutableMap[K, V]]
}
