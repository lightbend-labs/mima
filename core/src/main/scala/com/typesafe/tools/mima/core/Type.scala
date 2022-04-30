package com.typesafe.tools.mima.core

private[core] object Type {
  val byteType    = ValueType("Byte")
  val shortType   = ValueType("Short")
  val charType    = ValueType("Char")
  val intType     = ValueType("Int")
  val longType    = ValueType("Long")
  val floatType   = ValueType("Float")
  val doubleType  = ValueType("Double")
  val booleanType = ValueType("Boolean")
  val unitType    = ValueType("Unit")
}

sealed abstract private[core] class Type {
  def resultType: Type = throw new UnsupportedOperationException

  final override def toString = this match {
    case ValueType(name)                => name
    case ClassType(clazz)               => ClassInfo.formatClassName(clazz.fullName) // formattedFullName?
    case ArrayType(elemType)            => s"Array[$elemType]"
    case MethodType(paramTypes, resTpe) => paramTypes.mkString("(", ",", s")$resTpe")
  }
}

final private[core] case class ValueType(name: String) extends Type
final private[core] case class ClassType(private val clazz: ClassInfo) extends Type
final private[core] case class ArrayType(elemType: Type) extends Type
final private[core] case class MethodType(paramTypes: List[Type], override val resultType: Type) extends Type
