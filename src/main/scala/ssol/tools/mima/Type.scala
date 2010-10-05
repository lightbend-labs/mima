package ssol.tools.mima

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

  def fromSig(sig: String): Type = {

    var in = 0
    
    def getType(): Type = {
      val ch = sig(in)
      in += 1
      abbrevToValueType get ch match {
        case Some(tp) => 
          tp
        case None =>
          if (ch == '[') {
            ArrayType(getType())
          } else if (ch == 'L') {
            val end = sig indexOf (';', in)
            val fullname = sig.substring(in, end) replace ('/', '.')
            in = end + 1
            ClassType(ClassInfo.fromName(fullname))
          } else if (ch == '(') {
            val params = getParamTypes()
            in += 1
            MethodType(params, getType())
          } else {
            throw new MatchError("unknown signature: "+sig.substring(in))
          }
      }
    }

    def getParamTypes(): List[Type] =
      if (sig(in) == ')') List()
      else getType() :: getParamTypes()

    getType()
  }
}

abstract class Type {
  def elemType: Type = throw new UnsupportedOperationException
  def resultType: Type = throw new UnsupportedOperationException
}

case class ValueType(name: String) extends Type {
  override def toString = name
}
case class ClassType(clazz: ClassInfo) extends Type {
  override def toString = ClassInfo.formatClassName(clazz.fullName)
}
case class ArrayType(override val elemType: Type) extends Type {
  override def toString = "Array["+elemType+"]"
}
case class MethodType(paramTypes: List[Type], override val resultType: Type) extends Type {
  override def toString = paramTypes.mkString("(", ",", ")"+resultType)
}

