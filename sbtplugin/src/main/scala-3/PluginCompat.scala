package com.typesafe.tools.mima
package plugin

import sbt.*
import xsbti.{ FileConverter, HashedVirtualFileRef }

object PluginCompat:
  inline def toOldClasspath(cp: Seq[Attributed[HashedVirtualFileRef]])(using conv: FileConverter): Seq[Attributed[File]] =
    cp.map(_.map(x => conv.toPath(x).toFile))

  // Used to differentiate unset mimaPreviousArtifacts from empty mimaPreviousArtifacts
  private[plugin] object NoPreviousArtifacts extends EmptySet[ModuleID]
  private[plugin] object NoPreviousClassfiles extends EmptyMap[ModuleID, File]

  private[plugin] sealed class EmptySet[A] extends Set[A]:
    def iterator          = Iterator.empty
    def contains(elem: A) = false
    def excl(elem: A)     = this
    def incl(elem: A)     = Set(elem)

    override def size                  = 0
    override def foreach[U](f: A => U) = ()
    override def toSet[B >: A]: Set[B] = this.asInstanceOf[Set[B]]

  private[plugin] sealed class EmptyMap[K, V] extends Map[K, V]:
    def get(key: K)              = None
    def iterator                 = Iterator.empty
    def removed(key: K)          = this

    override def size                                       = 0
    override def contains(key: K)                           = false
    override def getOrElse[V1 >: V](key: K, default: => V1) = default
    override def updated[V1 >: V](key: K, value: V1)        = Map(key -> value)

    override def apply(key: K) = throw new NoSuchElementException(s"key not found: $key")
end PluginCompat
