package com.typesafe.tools.mima.core

object Type {
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

sealed abstract class Type {
  def resultType: Type = throw new UnsupportedOperationException

  override def toString = this match {
    case ValueType(name)                => name
    case ClassType(clazz)               => ClassInfo.formatClassName(clazz.fullName) // formattedFullName?
    case ArrayType(elemType)            => s"Array[$elemType]"
    case MethodType(paramTypes, resTpe) => paramTypes.mkString("(", ",", s")$resTpe")
  }
}

final case class ValueType(name: String)                                           extends Type
final case class ClassType(private val clazz: ClassInfo)                           extends Type
final case class ArrayType(elemType: Type)                                         extends Type
final case class MethodType(paramTypes: List[Type], override val resultType: Type) extends Type
