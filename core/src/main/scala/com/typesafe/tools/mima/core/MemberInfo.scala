package com.typesafe.tools.mima.core

object MemberInfo {

 /** The index of the string $_setter_$ in this string */
  private def setterIdx(name: String) = name.indexOf(setterTag)

  private val setterTag = "$_setter_$"
  private val setterSuffix = "_$eq"

  val ConstructorName = "<init>"

  def maybeSetter(name: String) = name.endsWith(setterSuffix)
}

class MemberInfo(val owner: ClassInfo, val bytecodeName: String, override val flags: Int, val descriptor: String) extends HasDeclarationName with WithAccessFlags {
  override def toString = "def " + bytecodeName + ": "+ descriptor

  def fieldString = "field "+decodedName+" in "+owner.classString
  def shortMethodString =
    (if(hasSyntheticName) (if(isExtensionMethod) "extension " else "synthetic ") else "") +
    (if(isDeprecated) "deprecated " else "") + "method "+decodedName + tpe
  def methodString = shortMethodString + " in " + owner.classString
  def memberString = if (isMethod) methodString else fieldString
  def defString = (if(isDeprecated) "@deprecated " else "") + "def " + decodedName + params.mkString("(", ",", ")") + ": " + tpe.resultType + " = "
  def applyString = decodedName + params.mkString("(", ",", ")")

  lazy val params: List[String] = tpe match {
    case MethodType(paramTypes, resultType) =>
      for ((ptype, index) <- paramTypes.zipWithIndex) yield "par" + index + ": " + ptype
  }

  def fullName = owner.formattedFullName + "." + decodedName

  def tpe: Type = owner.owner.definitions.fromDescriptor(descriptor)

  def staticImpl = owner.implClass.staticImpl(this)

  def isMethod: Boolean = descriptor(0) == '('

  def parametersDesc = {
    assert(isMethod)
    descriptor substring (1, descriptor indexOf ")")
  }

  def matchesType(other: MemberInfo): Boolean =
    if (isMethod) other.isMethod && parametersDesc == other.parametersDesc
    else !other.isMethod && descriptor == other.descriptor

  def resultDesc = {
    assert(descriptor(0) == '(')
    descriptor substring ((descriptor indexOf ")") + 1)
  }


  var codeOpt: Option[(Int, Int)] = None

  def isClassConstructor = bytecodeName == "<init>"

  def needCode = isClassConstructor

  import MemberInfo._

  var isTraitSetter = maybeSetter(bytecodeName) && setterIdx(bytecodeName) >= 0

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

  /** The name of the getter corresponding to this setter */
  private def getterName: String = {
    val sidx = setterIdx(bytecodeName)
    val start = if (sidx >= 0) sidx + setterTag.length else 0
    bytecodeName.substring(start, bytecodeName.length - setterSuffix.length)
  }

  /** The getter that corresponds to this setter */
  def getter: MemberInfo = {
    val argsig = "()" + parametersDesc
    owner.methods.get(getterName).find(_.descriptor == argsig).get
  }

  def description: String = bytecodeName + ": " + descriptor + " from " + owner.description

  @deprecated("Use 'descriptor' to get the type without, or 'signature' to get the type with generics", "0.3.1")
  val sig = descriptor
  @deprecated("Replaced with 'parametersDesc'", "0.3.1")
  def parametersSig = parametersDesc
  @deprecated("Replaced with 'resultDesc'", "0.3.1")
  def resultSig = resultDesc
}

