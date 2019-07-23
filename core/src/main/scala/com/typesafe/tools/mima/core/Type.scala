package com.typesafe.tools.mima.core

object Type {

  val byteType = ValueType("Byte")
  val shortType = ValueType("Short")
  val charType = ValueType("Char")
  val intType = ValueType("Int")
  val longType = ValueType("Long")
  val floatType = ValueType("Float")
  val doubleType = ValueType("Double")
  val booleanType = ValueType("Boolean")
  val unitType = ValueType("Unit")

  val abbrevToValueType = Map(
    'B' -> byteType,
    'S' -> shortType,
    'C' -> charType,
    'I' -> intType,
    'J' -> longType,
    'F' -> floatType,
    'D' -> doubleType,
    'Z' -> booleanType,
    'V' -> unitType)
}

abstract class Type {
  def resultType: Type = throw new UnsupportedOperationException
}

case class ValueType(name: String) extends Type {
  override def toString = name
}
case class ClassType(private val clazz: ClassInfo) extends Type {
  override def toString = ClassInfo.formatClassName(clazz.fullName)
}
case class ArrayType(elemType: Type) extends Type {
  override def toString = "Array["+elemType+"]"
}
case class MethodType(paramTypes: List[Type], override val resultType: Type) extends Type {
  override def toString = paramTypes.mkString("(", ",", ")"+resultType)
}

