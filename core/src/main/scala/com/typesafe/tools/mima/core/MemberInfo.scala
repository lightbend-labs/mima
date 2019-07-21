package com.typesafe.tools.mima.core

object MemberInfo {
  val ConstructorName = "<init>"
}

class MemberInfo(val owner: ClassInfo, val bytecodeName: String, override val flags: Int, val descriptor: String) extends HasDeclarationName with WithAccessFlags {
  override def toString = "def " + bytecodeName + ": "+ descriptor

  def fieldString = "field "+decodedName+" in "+owner.classString
  def shortMethodString =
    (if (hasSyntheticName) (if (isExtensionMethod) "extension " else "synthetic ") else "") +
    (if (isDeprecated) "deprecated " else "") + "method "+decodedName + tpe
  def methodString = shortMethodString + " in " + owner.classString
  def memberString = if (isMethod) methodString else fieldString

  lazy val params: List[String] = tpe match {
    case MethodType(paramTypes, _) =>
      for ((ptype, index) <- paramTypes.zipWithIndex) yield "par" + index + ": " + ptype
  }

  def fullName = owner.formattedFullName + "." + decodedName

  def tpe: Type = owner.owner.definitions.fromDescriptor(descriptor)

  def isMethod: Boolean = descriptor(0) == '('

  def parametersDesc = {
    assert(isMethod)
    descriptor substring (1, descriptor indexOf ")")
  }

  def matchesType(other: MemberInfo): Boolean =
    if (isMethod) other.isMethod && parametersDesc == other.parametersDesc
    else !other.isMethod && descriptor == other.descriptor

  var isDeprecated = false

  // The full 'Signature' attribute, which includes generics.
  // For type without generics, see the 'descriptor'
  var signature = ""

  def hasSyntheticName: Boolean = decodedName contains '$'

  def isExtensionMethod: Boolean = {
    var i = decodedName.length-1
    while(i >= 0 && Character.isDigit(decodedName.charAt(i))) i -= 1
    decodedName.substring(0, i+1).endsWith("$extension")
  }

  def isDefaultGetter: Boolean = decodedName.contains("$default$")

  def isAccessible: Boolean = isPublic && !isSynthetic && (!hasSyntheticName || isExtensionMethod || isDefaultGetter)

  def nonAccessible: Boolean = !isAccessible

  def isStatic: Boolean = ClassfileParser.isStatic(flags)

  def description: String = bytecodeName + ": " + descriptor + " from " + owner.description
}
