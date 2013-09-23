package com.typesafe.tools.mima.core

import scala.reflect.NameTransformer

trait HasDeclarationName {
  /** The name as found in the bytecode. */
  def bytecodeName: String

  /** The name as found in the original Scala source. */
  final def decodedName: String = NameTransformer.decode(bytecodeName)
}