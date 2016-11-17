package com.typesafe.tools.mima.core

import scala.reflect.NameTransformer

object MemberInfo {

 /** The index of the string $_setter_$ in this string */
  private def setterIdx(name: String) = name.indexOf(setterTag)

  private val setterTag = "$_setter_$"
  private val setterSuffix = "_$eq"

  val ConstructorName = "<init>"

  def maybeSetter(name: String) = name.endsWith(setterSuffix)
}

class MemberInfo(val owner: ClassInfo, val bytecodeName: String, override val flags: Int, val sig: String) extends HasDeclarationName with WithAccessFlags {
  override def toString = "def " + bytecodeName + ": "+ sig

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

  def tpe: Type = owner.owner.definitions.fromSig(sig)

  def staticImpl = owner.implClass.staticImpl(this)

  def isMethod: Boolean = sig(0) == '('

  def parametersSig = {
    assert(isMethod)
    sig substring (1, sig indexOf ")")
  }

  def matchesType(other: MemberInfo): Boolean =
    if (isMethod) other.isMethod && parametersSig == other.parametersSig
    else !other.isMethod && sig == other.sig

  def resultSig = {
    assert(sig(0) == '(')
    sig substring ((sig indexOf ")") + 1)
  }


  var codeOpt: Option[(Int, Int)] = None

  def isClassConstructor = bytecodeName == "<init>"

  def needCode = isClassConstructor

  import MemberInfo._

  var isTraitSetter = maybeSetter(bytecodeName) && setterIdx(bytecodeName) >= 0

  var isDeprecated = false

  def hasSyntheticName: Boolean = decodedName contains '$'

  def isExtensionMethod: Boolean = {
    var i = decodedName.length-1
    while(i >= 0 && Character.isDigit(decodedName.charAt(i))) i -= 1
    decodedName.substring(0, i+1).endsWith("$extension")
  }

  def isAccessible: Boolean = isPublic && !isSynthetic && (!hasSyntheticName || isExtensionMethod)

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
    val argsig = "()" + parametersSig
    owner.methods.get(getterName).find(_.sig == argsig).get
  }

  def description: String = bytecodeName + ": " + sig + " from " + owner.description
}

